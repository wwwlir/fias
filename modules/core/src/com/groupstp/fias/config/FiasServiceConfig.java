package com.groupstp.fias.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.Default;

@Source(type = SourceType.DATABASE)
public interface FiasServiceConfig extends Config {

    @Property("fias.fias-service.path")
    @Default("/mnt/sda2/lobo/fias/xml")
    String getPath();
}
