package com.groupstp.fias.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.Default;
import com.haulmont.cuba.core.config.defaults.DefaultInt;
import com.haulmont.cuba.core.config.defaults.DefaultInteger;
import com.haulmont.cuba.core.config.defaults.DefaultLong;

@Source(type = SourceType.DATABASE)
public interface FiasServiceConfig extends Config {

    @Property("fias.fias-service.path")
    @Default("D:/Work/Files/fias/fias_xml")
    String getPath();

    @Property("fias.fias-service.batchSize")
    @DefaultInt(1000)
    int getBatchSize();

    @Property("fias.fias-service.import-process.progressRegions")
    @DefaultLong(0)
    long getProgressRegions();
    void setProgressRegions(long progress);

    @Property("fias.fias-service.import-process.progressAutonomies")
    @DefaultLong(0)
    long getProgressAutonomies();
    void setProgressAutonomies(long progress);

    @Property("fias.fias-service.import-process.progressArea")
    @DefaultLong(0)
    long getProgressArea();
    void setProgressArea(long progress);

    @Property("fias.fias-service.import-process.progressCity")
    @DefaultLong(0)
    long getProgressCity();
    void setProgressCity(long progress);

    @Property("fias.fias-service.import-process.progressCommunity")
    @DefaultLong(0)
    long getProgressCommunity();
    void setProgressCommunity(long progress);

    @Property("fias.fias-service.import-process.progressLocation")
    @DefaultLong(0)
    long getProgressLocation();
    void setProgressLocation(long progress);

    @Property("fias.fias-service.import-process.progressStreet")
    @DefaultLong(0)
    long getProgressStreet();
    void setProgressStreet(long progress);

    @Property("fias.fias-service.import-process.progressHouses")
    @DefaultLong(0)
    long getProgressHouses();
    void setProgressHouses(long progress);
}
