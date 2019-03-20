package ru.groupstp.fias.entity;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.InheritanceType;
import javax.persistence.Inheritance;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import com.haulmont.cuba.core.entity.StandardEntity;

@Entity(name = "fias$City")
public class City extends FiasEntity {
    private static final long serialVersionUID = 7044495384702566447L;


}