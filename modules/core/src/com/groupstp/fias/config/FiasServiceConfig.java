package com.groupstp.fias.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.Default;
import com.haulmont.cuba.core.config.defaults.DefaultInt;
import com.haulmont.cuba.core.config.defaults.DefaultLong;

@Source(type = SourceType.DATABASE)
public interface FiasServiceConfig extends Config {

    @Property("fias.fias-service.path")
    @Default("D:/Work/Files/fias/fias_xml")
    String getPath();

    @Property("fias.fias-service.import-process.progressRegions")
    @DefaultLong(0)
    int getProgressRegions();
    void setProgressRegions(int progress);

    @Property("fias.fias-service.import-process.progressAutonomies")
    @DefaultLong(0)
    int getProgressAutonomies();
    void setProgressAutonomies(int progress);

    @Property("fias.fias-service.import-process.progressArea")
    @DefaultLong(0)
    int getProgressArea();
    void setProgressArea(int progress);

    @Property("fias.fias-service.import-process.progressCity")
    @DefaultLong(0)
    int getProgressCity();
    void setProgressCity(int progress);

    @Property("fias.fias-service.import-process.progressCommunity")
    @DefaultLong(0)
    int getProgressCommunity();
    void setProgressCommunity(int progress);

    @Property("fias.fias-service.import-process.progressLocation")
    @DefaultLong(0)
    int getProgressLocation();
    void setProgressLocation(int progress);

    @Property("fias.fias-service.import-process.progressStreet")
    @DefaultLong(0)
    int getProgressStreet();
    void setProgressStreet(int progress);

    @Property("fias.fias-service.import-process.progressHouses")
    @DefaultLong(0)
    int getProgressHouses();
    void setProgressHouses(int progress);
}
