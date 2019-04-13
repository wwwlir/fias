package com.groupstp.fias.web.fiasentity;

import com.groupstp.fias.entity.FiasEntity;
import com.haulmont.cuba.gui.components.AbstractEditor;
import com.haulmont.cuba.gui.components.PickerField;

import javax.inject.Named;
import java.util.Map;

public class FiasEntityEdit extends AbstractEditor<FiasEntity> {
    @Named("fieldGroup.parent")
    private PickerField parentField;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);
        parentField.addOpenAction().setEditScreen("fias$FiasEntity.edit");
    }
}