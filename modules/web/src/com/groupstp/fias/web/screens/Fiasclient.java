package com.groupstp.fias.web.screens;

import com.groupstp.fias.entity.FiasEntity;
import com.groupstp.fias.service.FiasReadService;
import com.haulmont.cuba.gui.components.AbstractWindow;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.components.ProgressBar;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundTaskHandler;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import org.meridor.fias.enums.AddressLevel;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Fiasclient extends AbstractWindow {
    @Inject
    private FiasReadService fiasReadService;
    @Inject
    private BackgroundWorker backgroundWorker;

    @Inject
    private CheckBox regionCheckField;
    @Inject
    private CheckBox autonomyCheckField;
    @Inject
    private CheckBox areaCheckField;
    @Inject
    private CheckBox cityCheckField;
    @Inject
    private CheckBox communityCheckField;
    @Inject
    private CheckBox locationCheckField;
    @Inject
    private CheckBox streetCheckField;
    @Inject
    private CheckBox houseCheckField;
    @Inject
    private LookupField regionField;
    @Inject
    private LookupField cityField;
    @Inject
    private ProgressBar progressBar;

    private BackgroundTaskHandler taskHandler;

    public void onBtnClick() {
        HashMap<Object, Object> levelMap = new HashMap<>();
        levelMap.put(AddressLevel.REGION, regionCheckField.getValue());
        levelMap.put(AddressLevel.AUTONOMY, autonomyCheckField.getValue());
        levelMap.put(AddressLevel.AREA, areaCheckField.getValue());
        levelMap.put(AddressLevel.CITY, cityCheckField.getValue());
        levelMap.put(AddressLevel.COMMUNITY, communityCheckField.getValue());
        levelMap.put(AddressLevel.LOCATION, locationCheckField.getValue());
        levelMap.put(AddressLevel.STREET, streetCheckField.getValue());
        levelMap.put("needLoadHouses", houseCheckField.getValue());
        if (regionField.getValue() != null)
            levelMap.put("regionId", ((FiasEntity) regionField.getValue()).getId());
        if (cityField.getValue() != null)
            levelMap.put("cityId", ((FiasEntity) cityField.getValue()).getId());
        progressBar.setIndeterminate(true);
        taskHandler = backgroundWorker.handle(createBackgroundTask(levelMap));
        taskHandler.execute();
    }

    private BackgroundTask<Integer, Void> createBackgroundTask(HashMap<Object, Object> levelMap) {
        return new BackgroundTask<Integer, Void>(TimeUnit.HOURS.toSeconds(5), this) {
            @Override
            public Void run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {
                fiasReadService.readFias(levelMap);
                taskLifeCycle.publish(1);
                return null;
            }

            @Override
            public void done(Void result) {
                showNotification(getMessage("loadDone"));
                progressBar.setIndeterminate(false);
                super.done(result);
            }

            @Override
            public void progress(List<Integer> changes) {
                progressBar.setIndeterminate(false);
                super.progress(changes);
            }

            @Override
            public void canceled() {
                progressBar.setIndeterminate(false);
                showNotification("Задача была отменена");
            }
        };
    }

    public void onPauseLoadingDataBtnClick() {
        //if (backgroundWorker.handle(task).isAlive())
        taskHandler.cancel();
    }
}