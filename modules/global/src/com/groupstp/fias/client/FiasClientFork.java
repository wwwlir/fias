package com.groupstp.fias.client;


import org.meridor.fias.AddressObjects;
import org.meridor.fias.loader.PartialUnmarshaller;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

public class FiasClientFork {
    private final XMLLoaderFork xmlLoaderFork;

    public FiasClientFork(Path xmlDirectory) throws FileNotFoundException {
        if (!Files.exists(xmlDirectory)) {
            throw new FileNotFoundException(String.format(
                    "Specified path [%s] does not exist",
                    xmlDirectory
            ));
        }
        xmlLoaderFork = new XMLLoaderFork(xmlDirectory);
    }

    public <T> PartialUnmarshaller<T> getUnmarshaller(Class<T> clazz) {
        return xmlLoaderFork.getUnmarshaller(clazz);
    }

    public <T> PartialUnmarshallerFork<T> getUnmarshallerFork(Class<T> clazz, long offset) {
        return xmlLoaderFork.getUnmarshallerFork(clazz, offset);
    }

    @Deprecated
    public List<AddressObjects.Object> load(Predicate<AddressObjects.Object> predicate) {
        return xmlLoaderFork.loadRaw(predicate);
    }

    public AddressObjectFork load(Predicate<AddressObjects.Object> predicate, Path filePath, long offset) {
        return xmlLoaderFork.loadObject(predicate, filePath, offset);
    }

    public AddressObjectFork load(Predicate<AddressObjects.Object> predicate, ProgressCounterFilterInputStream inputStream, long offset) {
        return xmlLoaderFork.loadObject(predicate, inputStream, offset);
    }

    public List<AddressObjectFork> loadList(Predicate<AddressObjects.Object> predicate, Path filePath, long offset, int batchSize) throws FileNotFoundException {
        return xmlLoaderFork.loadObjects(predicate, filePath, offset, batchSize);
    }
}
