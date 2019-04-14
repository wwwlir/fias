package com.groupstp.fias.entity.enums;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;


public enum FiasEntityOperationStatus implements EnumClass<Integer> {

    INIT(1),
    ADD(10),
    EDIT(20),
    EDIT_GROUP(21),
    REMOVE(30),
    REMOVE_CASCADE(31),
    MERGE(40),
    MERGE_PARENT(41),
    MERGE_REMOVE(42),
    MERGE_INIT(43),
    CHANGE_PARENT(50),
    CHANGE_PARENT_CASCADE(51),
    SPLIT_REMOVE(60),
    SPLIT_INIT(61),
    RESTORE(70);

    private Integer id;

    FiasEntityOperationStatus(Integer value) {
        this.id = value;
    }

    public Integer getId() {
        return id;
    }

    @Nullable
    public static FiasEntityOperationStatus fromId(Integer id) {
        for (FiasEntityOperationStatus at : FiasEntityOperationStatus.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}