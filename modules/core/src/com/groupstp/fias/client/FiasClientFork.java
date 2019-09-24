package com.groupstp.fias.client;


import org.meridor.fias.AddressObjects;
import org.meridor.fias.loader.PartialUnmarshaller;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

public class FiasClientFork {
    //private final XMLLoader xmlLoader;
    private final XMLLoader fias_XMLLoader;

    public FiasClientFork(Path xmlDirectory) throws FileNotFoundException {
        if (!Files.exists(xmlDirectory)) {
            throw new FileNotFoundException(String.format(
                    "Specified path [%s] does not exist",
                    xmlDirectory
            ));
        }
        //xmlLoader = new XMLLoader(xmlDirectory);
        fias_XMLLoader = new XMLLoader(xmlDirectory);
    }

    public <T> PartialUnmarshaller<T> getUnmarshaller(Class<T> clazz) {
        return fias_XMLLoader.getUnmarshaller(clazz);
    }

    public List<AddressObjects.Object> load(Predicate<AddressObjects.Object> predicate) {
        return fias_XMLLoader.loadRaw(predicate);
    }
}
