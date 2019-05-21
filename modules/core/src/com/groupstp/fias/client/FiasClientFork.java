package com.groupstp.fias.client;


import org.meridor.fias.loader.PartialUnmarshaller;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FiasClientFork {
    private final XMLLoader xmlLoader;

    public FiasClientFork(Path xmlDirectory) throws FileNotFoundException {
        if (!Files.exists(xmlDirectory)) {
            throw new FileNotFoundException(String.format(
                    "Specified path [%s] does not exist",
                    xmlDirectory
            ));
        }
        xmlLoader = new XMLLoader(xmlDirectory);
    }

    public <T> PartialUnmarshaller<T> getUnmarshaller(Class<T> clazz) {
        return xmlLoader.getUnmarshaller(clazz);
    }
}
