package ru.groupstp.fias.entity;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.chile.core.annotations.NamePattern;
import javax.validation.constraints.NotNull;

@NamePattern(" |")
@Entity(name = "fias$Region")
public class Region extends FiasEntity {
    private static final long serialVersionUID = 4161402112705818548L;



}