package com.groupstp.fias.client;

import org.meridor.fias.loader.PartialUnmarshaller;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.meridor.fias.enums.FiasFile.HOUSE;

public class XMLLoader {
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

}
