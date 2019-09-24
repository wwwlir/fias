package com.groupstp.fias.client;

import com.groupstp.fias.service.FiasReadService;
import org.meridor.fias.AddressObjects;
import org.meridor.fias.loader.PartialUnmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.meridor.fias.enums.FiasFile.ADDRESS_OBJECTS;
import static org.meridor.fias.enums.FiasFile.HOUSE;

public class XMLLoader {
    private static final Logger log = LoggerFactory.getLogger(FiasReadService.class);

    private final Path xmlDirectory;

    public XMLLoader(Path xmlDirectory) {
        this.xmlDirectory = xmlDirectory;
    }

    private Path getPathByPattern(String startsWith) throws IOException {
        Optional<Path> filePath = Files.list(xmlDirectory)
                .map(xmlDirectory::relativize)
                .filter(path -> path.toString().startsWith(startsWith) && path.toString().toLowerCase().endsWith("xml"))
                .findFirst();
        if (!filePath.isPresent()) {
            throw new FileNotFoundException(String.format("Can't find XML file with name starting with [%s]", startsWith));
        }
        return xmlDirectory.resolve(filePath.get());
    }

    public <T> PartialUnmarshaller<T> getUnmarshaller(Class<T> clazz) {
        try {
            Path filePath = getPathByPattern(HOUSE.getName());
            InputStream inputStream = new BufferedInputStream(new FileInputStream(filePath.toFile()));
            return new PartialUnmarshaller<>(inputStream, clazz);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public List<AddressObjects.Object> loadRaw(Predicate<AddressObjects.Object> predicate) {
        if (predicate == null) {
            return Collections.emptyList();
        }
        try {
            Path filePath = getPathByPattern(ADDRESS_OBJECTS.getName());
            //InputStream inputStream = new BufferedInputStream(new FileInputStream(filePath.toFile()));
            ProgressCounterFilterInputStream inputStream = new ProgressCounterFilterInputStream(new BufferedInputStream(new FileInputStream(filePath.toFile())));
            log.debug("Searching objects in file {}", filePath);
            try (PartialUnmarshallerFork<AddressObjects.Object> partialUnmarshaller = new PartialUnmarshallerFork<>(inputStream, AddressObjects.Object.class)) {
                List<AddressObjects.Object> results = new ArrayList<>();
                while (partialUnmarshaller.hasNext()) {
                    AddressObjects.Object addressObject = partialUnmarshaller.next();
                    if (predicate.test(addressObject)) {
                        results.add(addressObject);
                        log.debug("Founded {} object(s) in file, readed {} % of file",
                                results.indexOf(addressObject),
                                //(int) (Math.abs((double) partialUnmarshaller.getReader().getLocation().getCharacterOffset()) / (double) Files.size(filePath) * 100));
                                (int) (Math.abs((double) partialUnmarshaller.getInputStream().getProgress() / (double) Files.size(filePath) * 100)));
                    }
                }
                return results;
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
