package ru.groupstp.fias.web.screens;

import com.haulmont.cuba.gui.components.AbstractFrame;
import com.haulmont.cuba.gui.components.AbstractWindow;
import ru.groupstp.fias.service.FiasReadService;

import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Fiasclient extends AbstractWindow {

    @Inject
    private FiasReadService fiasReadService;

    public void onBtnClick() throws FileNotFoundException {
        fiasReadService.readFias();
    }
}