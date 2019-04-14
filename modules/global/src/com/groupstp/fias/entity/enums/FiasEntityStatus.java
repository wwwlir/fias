package com.groupstp.fias.entity.enums;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;


public enum FiasEntityStatus implements EnumClass<Integer> {

    NOT_ACTUAL(0),
    ACTUAL(1);

    private Integer id;

    FiasEntityStatus(Integer value) {
        this.id = value;
    }

    public Integer getId() {
        return id;
    }

    @Nullable
    public static FiasEntityStatus fromId(Integer id) {
        for (FiasEntityStatus at : FiasEntityStatus.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}