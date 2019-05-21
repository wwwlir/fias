package com.groupstp.fias.service;


import java.io.FileNotFoundException;
import java.util.Map;

public interface FiasReadService {
    String NAME = "fias_FiasReadService";

    void readFias() throws FileNotFoundException;

    void readFias(Map<Object, Object> options) throws FileNotFoundException;

}