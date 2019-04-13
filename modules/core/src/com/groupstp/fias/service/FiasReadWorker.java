package com.groupstp.fias.service;

import java.io.FileNotFoundException;

public interface FiasReadWorker {
    String NAME = "fias_FiasReadWorker";

    public void readFias() throws FileNotFoundException;
}
