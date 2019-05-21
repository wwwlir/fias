package com.groupstp.fias.entity;

import com.groupstp.fias.entity.enums.FiasEntityOperationStatus;
import com.groupstp.fias.entity.enums.FiasEntityStatus;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.*;
import java.util.Date;

@NamePattern("%s %s|shortname,name")
@Table(name = "FIAS_FIAS_ENTITY")
@Entity(name = "fias$FiasEntity")
public class FiasEntity extends StandardEntity {
    private static final long serialVersionUID = 5234139283100152959L;

    @Column(name = "NAME")
    protected String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_ID")
    protected FiasEntity parent;

    @Column(name = "POSTAL_CODE", length = 6)
    protected String postalCode;

    @Column(name = "OFFNAME")
    protected String offname;

    @Column(name = "FORMALNAME")
    protected String formalname;

    @Lob
    @Column(name = "POSSIBLE_NAMES")
    protected String possibleNames;

    @Column(name = "CODE")
    protected String code;

    @Column(name = "SHORTNAME", length = 10)
    protected String shortname;


    @Temporal(TemporalType.DATE)
    @Column(name = "UPDATEDATE")
    protected Date updatedate;

    @Column(name = "ACTSTATUS")
    protected Integer actstatus;

    @Column(name = "OPERSTATUS")
    protected Integer operstatus;

    @Temporal(TemporalType.DATE)
    @Column(name = "STARTDATE")
    protected Date startdate;

    @Temporal(TemporalType.DATE)
    @Column(name = "ENDDATE")
    protected Date enddate;

    public Date getEnddate() {
        return enddate;
    }

    public void setEnddate(Date enddate) {
        this.enddate = enddate;
    }


    public void setStartdate(Date startdate) {
        this.startdate = startdate;
    }

    public Date getStartdate() {
        return startdate;
    }


    public void setActstatus(FiasEntityStatus actstatus) {
        this.actstatus = actstatus == null ? null : actstatus.getId();
    }

    public FiasEntityStatus getActstatus() {
        return actstatus == null ? null : FiasEntityStatus.fromId(actstatus);
    }

    public void setOperstatus(FiasEntityOperationStatus operstatus) {
        this.operstatus = operstatus == null ? null : operstatus.getId();
    }

    public FiasEntityOperationStatus getOperstatus() {
        return operstatus == null ? null : FiasEntityOperationStatus.fromId(operstatus);
    }


    public void setUpdatedate(Date updatedate) {
        this.updatedate = updatedate;
    }

    public Date getUpdatedate() {
        return updatedate;
    }


    public void setParent(FiasEntity parent) {
        this.parent = parent;
    }

    public FiasEntity getParent() {
        return parent;
    }


    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getPostalCode() {
        return postalCode;
    }


    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setOffname(String offname) {
        this.offname = offname;
    }

    public String getOffname() {
        return offname;
    }

    public void setFormalname(String formalname) {
        this.formalname = formalname;
    }

    public String getFormalname() {
        return formalname;
    }

    public void setPossibleNames(String possibleNames) {
        this.possibleNames = possibleNames;
    }

    public String getPossibleNames() {
        return possibleNames;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setShortname(String shortname) {
        this.shortname = shortname;
    }

    public String getShortname() {
        return shortname;
    }


}