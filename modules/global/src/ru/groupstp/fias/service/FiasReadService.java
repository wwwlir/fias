package ru.groupstp.fias.service;


import java.io.FileNotFoundException;

public interface FiasReadService {
    String NAME = "fias_FiasReadService";

    void readFias() throws FileNotFoundException;
}