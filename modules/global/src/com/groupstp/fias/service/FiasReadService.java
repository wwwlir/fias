package com.groupstp.fias.service;


import com.groupstp.fias.entity.FiasEntity;
import com.groupstp.fias.entity.House;

import java.io.FileNotFoundException;
import java.util.Map;
import java.util.UUID;

public interface FiasReadService {
    String NAME = "fias_FiasReadService";

    void readFias() throws FileNotFoundException;

    void readFias(Map<Object, Object> options) throws FileNotFoundException;

    Map<Class, FiasEntity> getAddressComponents(House house);

    Map<Class, FiasEntity> getAddressComponents(UUID houseId);

}