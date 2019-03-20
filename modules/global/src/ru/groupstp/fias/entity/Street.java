package ru.groupstp.fias.entity;

import javax.persistence.Entity;
import javax.persistence.Table;
import com.haulmont.cuba.core.entity.StandardEntity;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity(name = "fias$Street")
public class Street extends FiasEntity {
    private static final long serialVersionUID = 7438597186299001947L;



}