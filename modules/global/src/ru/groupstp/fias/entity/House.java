package ru.groupstp.fias.entity;

import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Table(name = "FIAS_HOUSE")
@Entity(name = "fias$House")
public class House extends StandardEntity {
    private static final long serialVersionUID = 101923876676193777L;

    @Column(name = "POSTALCODE", length = 6)
    protected String postalcode;

    @Column(name = "IFNSFL", length = 4)
    protected String ifnsfl;

    @Column(name = "TERRIFNSFL", length = 4)
    protected String terrifnsfl;

    @Column(name = "IFNSUL", length = 4)
    protected String ifnsul;

    @Column(name = "TERRIFNSUL", length = 4)
    protected String terrifnsul;

    @Column(name = "OKATO", length = 11)
    protected String okato;

    @Column(name = "OKTMO", length = 11)
    protected String oktmo;

    @Column(name = "HOUSENUM", length = 20)
    protected String housenum;

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

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PARENT_ID")
    protected FiasEntity parent;

    public String getTerrifnsfl() {
        return terrifnsfl;
    }

    public void setTerrifnsfl(String terrifnsfl) {
        this.terrifnsfl = terrifnsfl;
    }

    public void setIfnsfl(String ifnsfl) {
        this.ifnsfl = ifnsfl;
    }

    public String getIfnsfl() {
        return ifnsfl;
    }

    public void setTerrifnsul(String terrifnsul) {
        this.terrifnsul = terrifnsul;
    }

    public String getTerrifnsul() {
        return terrifnsul;
    }

    public void setIfnsul(String ifnsul) {
        this.ifnsul = ifnsul;
    }

    public String getIfnsul() {
        return ifnsul;
    }

    public void setOkato(String okato) {
        this.okato = okato;
    }

    public String getOkato() {
        return okato;
    }

    public void setOktmo(String oktmo) {
        this.oktmo = oktmo;
    }

    public String getOktmo() {
        return oktmo;
    }

    public void setHousenum(String housenum) {
        this.housenum = housenum;
    }

    public String getHousenum() {
        return housenum;
    }


    public void setPostalcode(String postalcode) {
        this.postalcode = postalcode;
    }

    public String getPostalcode() {
        return postalcode;
    }


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