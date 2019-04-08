package ru.groupstp.fias.entity;

import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.*;
import java.util.Date;

@Table(name = "FIAS_HOUSE")
@Entity(name = "fias$House")
public class House extends StandardEntity {
    private static final long serialVersionUID = 101923876676193777L;

    @Column(name = "ESTSTATUS")
    protected Integer eststatus;

    @Column(name = "BUILDNUM", length = 10)
    protected String buildnum;

    @Column(name = "STRUCNUM", length = 10)
    protected String strucnum;

    @Column(name = "STRSTATUS")
    protected Integer strstatus;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "STARTDATE")
    protected Date startdate;

    @Temporal(TemporalType.DATE)
    @Column(name = "ENDDATE")
    protected Date enddate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_ID")
    protected FiasEntity parent;

    public void setStartdate(Date startdate) {
        this.startdate = startdate;
    }

    public Date getStartdate() {
        return startdate;
    }

    public void setEnddate(Date enddate) {
        this.enddate = enddate;
    }

    public Date getEnddate() {
        return enddate;
    }


    public void setStrucnum(String strucnum) {
        this.strucnum = strucnum;
    }

    public String getStrucnum() {
        return strucnum;
    }

    public void setStrstatus(Integer strstatus) {
        this.strstatus = strstatus;
    }

    public Integer getStrstatus() {
        return strstatus;
    }

    public void setParent(FiasEntity parent) {
        this.parent = parent;
    }

    public FiasEntity getParent() {
        return parent;
    }


    public void setEststatus(Integer eststatus) {
        this.eststatus = eststatus;
    }

    public Integer getEststatus() {
        return eststatus;
    }

    public void setBuildnum(String buildnum) {
        this.buildnum = buildnum;
    }

    public String getBuildnum() {
        return buildnum;
    }


}