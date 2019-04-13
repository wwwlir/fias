package ru.groupstp.fias.entity;

import com.haulmont.chile.core.annotations.NamePattern;

import javax.persistence.Entity;

@NamePattern("%s|name")
@Entity(name = "fias$Region")
public class Region extends FiasEntity {
    private static final long serialVersionUID = 4161402112705818548L;



}