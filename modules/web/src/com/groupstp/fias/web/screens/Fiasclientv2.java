package com.groupstp.fias.web.screens;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.groupstp.fias.client.AddressObjectFork;
import com.groupstp.fias.client.FiasClientFork;
import com.groupstp.fias.client.PartialUnmarshallerFork;
import com.groupstp.fias.config.FiasServiceConfig;
import com.groupstp.fias.entity.*;
import com.groupstp.fias.entity.enums.FiasEntityOperationStatus;
import com.groupstp.fias.entity.enums.FiasEntityStatus;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundTaskHandler;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import org.meridor.fias.AddressObjects;
import org.meridor.fias.Houses;
import org.meridor.fias.enums.AddressLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.meridor.fias.enums.FiasFile.ADDRESS_OBJECTS;
import static org.meridor.fias.enums.FiasFile.HOUSE;

public class Fiasclientv2 extends AbstractWindow {
    private static final Logger log = LoggerFactory.getLogger("FiasClient");
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
    @Inject
    private Label progressLabel;
    @Inject
    private Button loadDataBtn;
    @Inject
    private Button pauseLoadingDataBtn;
    @Inject
    private Button resetProgressButton;

    private BackgroundTaskHandler taskHandler;

    @Inject
    private DataManager dataManager;
    @Inject
    private Configuration configuration;

    private FiasClientFork fiasClient;
    private Path xmlDirectory;

    private long progress;

    private boolean taskWasStarted = false;
    private Class lastClassWorked;

    @Override
    public void init(Map<String, Object> params) {
        setupCloseWindowListeners();
    }

    public void onBtnClick() {
        HashMap<Object, Object> options = new HashMap<>();
        options.put(AddressLevel.REGION, regionCheckField.getValue());
        options.put(AddressLevel.AUTONOMY, autonomyCheckField.getValue());
        options.put(AddressLevel.AREA, areaCheckField.getValue());
        options.put(AddressLevel.CITY, cityCheckField.getValue());
        options.put(AddressLevel.COMMUNITY, communityCheckField.getValue());
        options.put(AddressLevel.LOCATION, locationCheckField.getValue());
        options.put(AddressLevel.STREET, streetCheckField.getValue());
        options.put("needLoadHouses", houseCheckField.getValue());
        if (regionField.getValue() != null)
            options.put("regionId", ((FiasEntity) regionField.getValue()).getId());
        if (cityField.getValue() != null)
            options.put("cityId", ((FiasEntity) cityField.getValue()).getId());

        taskHandler = backgroundWorker.handle(createBackgroundTask(options));
        taskHandler.execute();
        setupControlButtons(false);
    }

    private BackgroundTask<Integer, Void> createBackgroundTask(Map<Object, Object> options) {
        return new BackgroundTask<Integer, Void>(TimeUnit.HOURS.toSeconds(5), this) {
            int percentValue = 1;

            //Class<? extends FiasEntity> clazz;
            Class clazz;
            Function<AddressObjects.Object, String> getCodeFunction;
            Predicate<AddressObjects.Object> predicate;

            @Override
            public Void run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {
                taskWasStarted = true;
                String path = configuration.getConfig(FiasServiceConfig.class).getPath();
                int batchSize = configuration.getConfig(FiasServiceConfig.class).getBatchSize();
                UUID regionId = ((UUID) options.getOrDefault("regionId", null));
                UUID cityId = ((UUID) options.getOrDefault("cityId", null));
                xmlDirectory = Paths.get(path);
                fiasClient = new FiasClientFork(xmlDirectory);
                Path filePath = getPathByPattern(ADDRESS_OBJECTS.getName());

                //грузим Regions
                if ((boolean) options.getOrDefault(AddressLevel.REGION, true)) {
                    clazz = Region.class;
                    lastClassWorked = clazz;
                    sendToLog(MessageFormat.format("Start Creating objects of class {0}", clazz.getSimpleName()));
                    getCodeFunction = AddressObjects.Object::getREGIONCODE;
                    predicate = o -> o.getAOLEVEL().equals(AddressLevel.REGION.getAddressLevel());
                    progress = getConfigProgress(clazz);
                    percentValue = (int) (Math.abs((double) progress / (double) Files.size(filePath) * 100));
                    taskLifeCycle.publish(percentValue);
                    while (progress <= Files.size(filePath)) {
                        if (taskLifeCycle.isCancelled() || taskLifeCycle.isInterrupted()) {
                            //updateConfigProgress(clazz, progress);
                            break;
                        } else {
                            CommitContext commitContext = new CommitContext();
                            sendToLog("Searching next batch of objects...");
                            List<AddressObjectFork> addressObjectForks = fiasClient.loadList(predicate, filePath, progress, batchSize);
                            if (addressObjectForks.size() == 0) {
                                sendToLog(MessageFormat.format("{0} new Fias Entities were processed (class = {1}), reached 100% of file",
                                        addressObjectForks.size(),
                                        clazz.getSimpleName()));
                                break;
                            } else {
                                for (AddressObjectFork addressObjectFork : addressObjectForks) {
                                    FiasEntity fe = loadFiasEntity(clazz, addressObjectFork.getObject());
                                    fe.setCode(getCodeFunction.apply(addressObjectFork.getObject()));
                                    commitContext.addInstanceToCommit(fe);
                                    progress = addressObjectFork.getOffset();
                                    percentValue = (int) (Math.abs((double) progress / (double) Files.size(filePath) * 100));
                                    taskLifeCycle.publish(percentValue);
                                }
                                dataManager.commit(commitContext);
                                sendToLog(MessageFormat.format("{0} new Fias Entities were processed (class = {1}), reached {2}% of file",
                                        addressObjectForks.size(),
                                        clazz.getSimpleName(),
                                        percentValue));
                            }
                        }
                    }
                    sendToLog(MessageFormat.format("Finished Creating objects of class {0}", clazz.getSimpleName()));
                }

                //грузим Autonomies
                if ((boolean) options.getOrDefault(AddressLevel.AUTONOMY, true)) {
                    clazz = Autonomy.class;
                    lastClassWorked = clazz;
                    getCodeFunction = AddressObjects.Object::getCODE;
                    predicate = o -> o.getAOLEVEL().equals(AddressLevel.AUTONOMY.getAddressLevel()) && testParent(o.getPARENTGUID(), regionId);
                    sendToLog(MessageFormat.format("Start Creating objects of class {0}", clazz.getSimpleName()));
                    progress = getConfigProgress(clazz);
                    percentValue = (int) (Math.abs((double) progress / (double) Files.size(filePath) * 100));
                    taskLifeCycle.publish(percentValue);
                    while (progress <= Files.size(filePath)) {
                        if (taskLifeCycle.isCancelled() || taskLifeCycle.isInterrupted()) {
                            //updateConfigProgress(clazz, progress);
                            break;
                        } else {
                            CommitContext commitContext = new CommitContext();
                            sendToLog("Searching next batch of objects...");
                            List<AddressObjectFork> addressObjectForks = fiasClient.loadList(predicate, filePath, progress, batchSize);
                            if (addressObjectForks.size() == 0) {
                                sendToLog(MessageFormat.format("No new Fias Entities were processed (class = {1}), reached 100% of file",
                                        addressObjectForks.size(),
                                        clazz.getSimpleName()));
                                break;
                            } else {
                                for (AddressObjectFork addressObjectFork : addressObjectForks) {
                                    FiasEntity fe = loadFiasEntity(clazz, addressObjectFork.getObject());
                                    fe.setCode(getCodeFunction.apply(addressObjectFork.getObject()));
                                    commitContext.addInstanceToCommit(fe);
                                    progress = addressObjectFork.getOffset();
                                    percentValue = (int) (Math.abs((double) progress / (double) Files.size(filePath) * 100));
                                    taskLifeCycle.publish(percentValue);
                                }
                                dataManager.commit(commitContext);
                                sendToLog(MessageFormat.format("{0} new Fias Entities were processed (class = {1}), reached {2}% of file",
                                        addressObjectForks.size(),
                                        clazz.getSimpleName(),
                                        percentValue));
                            }
                        }
                    }
                    sendToLog(MessageFormat.format("Finished Creating objects of class {0}", clazz.getSimpleName()));
                }

                //грузим Areas
                if ((boolean) options.getOrDefault(AddressLevel.AREA, true)) {
                    clazz = Area.class;
                    lastClassWorked = clazz;
                    getCodeFunction = AddressObjects.Object::getAREACODE;
                    predicate = o -> o.getAOLEVEL().equals(AddressLevel.AREA.getAddressLevel()) && testParent(o.getPARENTGUID(), regionId);
                    sendToLog(MessageFormat.format("Start Creating objects of class {0}", clazz.getSimpleName()));
                    progress = getConfigProgress(clazz);
                    percentValue = (int) (Math.abs((double) progress / (double) Files.size(filePath) * 100));
                    taskLifeCycle.publish(percentValue);
                    while (progress <= Files.size(filePath)) {
                        if (taskLifeCycle.isCancelled() || taskLifeCycle.isInterrupted()) {
                            //updateConfigProgress(clazz, progress);
                            break;
                        } else {
                            CommitContext commitContext = new CommitContext();
                            sendToLog("Searching next batch of objects...");
                            List<AddressObjectFork> addressObjectForks = fiasClient.loadList(predicate, filePath, progress, batchSize);
                            if (addressObjectForks.size() == 0) {
                                sendToLog(MessageFormat.format("No new Fias Entities were processed (class = {1}), reached 100% of file",
                                        addressObjectForks.size(),
                                        clazz.getSimpleName()));
                                break;
                            } else {
                                for (AddressObjectFork addressObjectFork : addressObjectForks) {
                                    FiasEntity fe = loadFiasEntity(clazz, addressObjectFork.getObject());
                                    fe.setCode(getCodeFunction.apply(addressObjectFork.getObject()));
                                    commitContext.addInstanceToCommit(fe);
                                    progress = addressObjectFork.getOffset();
                                    percentValue = (int) (Math.abs((double) progress / (double) Files.size(filePath) * 100));
                                    taskLifeCycle.publish(percentValue);
                                }
                                dataManager.commit(commitContext);
                                sendToLog(MessageFormat.format("{0} new Fias Entities were processed (class = {1}), reached {2}% of file",
                                        addressObjectForks.size(),
                                        clazz.getSimpleName(),
                                        percentValue));
                            }
                        }
                    }
                    sendToLog(MessageFormat.format("Finished Creating objects of class {0}", clazz.getSimpleName()));
                }

                //грузим Cities
                if ((boolean) options.getOrDefault(AddressLevel.CITY, true)) {
                    clazz = City.class;
                    lastClassWorked = clazz;
                    getCodeFunction = AddressObjects.Object::getCITYCODE;
                    predicate = o -> o.getAOLEVEL().equals(AddressLevel.CITY.getAddressLevel()) && testParent(o.getPARENTGUID(), regionId);
                    sendToLog(MessageFormat.format("Start Creating objects of class {0}", clazz.getSimpleName()));
                    progress = getConfigProgress(clazz);
                    percentValue = (int) (Math.abs((double) progress / (double) Files.size(filePath) * 100));
                    taskLifeCycle.publish(percentValue);
                    while (progress <= Files.size(filePath)) {
                        if (taskLifeCycle.isCancelled() || taskLifeCycle.isInterrupted()) {
                            //updateConfigProgress(clazz, progress);
                            break;
                        } else {
                            CommitContext commitContext = new CommitContext();
                            sendToLog("Searching next batch of objects...");
                            List<AddressObjectFork> addressObjectForks = fiasClient.loadList(predicate, filePath, progress, batchSize);
                            if (addressObjectForks.size() == 0) {
                                sendToLog(MessageFormat.format("No new Fias Entities were processed (class = {1}), reached 100% of file",
                                        addressObjectForks.size(),
                                        clazz.getSimpleName()));
                                break;
                            } else {
                                for (AddressObjectFork addressObjectFork : addressObjectForks) {
                                    FiasEntity fe = loadFiasEntity(clazz, addressObjectFork.getObject());
                                    fe.setCode(getCodeFunction.apply(addressObjectFork.getObject()));
                                    commitContext.addInstanceToCommit(fe);
                                    progress = addressObjectFork.getOffset();
                                    percentValue = (int) (Math.abs((double) progress / (double) Files.size(filePath) * 100));
                                    taskLifeCycle.publish(percentValue);
                                }
                                dataManager.commit(commitContext);
                                sendToLog(MessageFormat.format("{0} new Fias Entities were processed (class = {1}), reached {2}% of file",
                                        addressObjectForks.size(),
                                        clazz.getSimpleName(),
                                        percentValue));
                            }
                        }
                    }
                    sendToLog(MessageFormat.format("Finished Creating objects of class {0}", clazz.getSimpleName()));
                }

                //грузим Communities
                if ((boolean) options.getOrDefault(AddressLevel.COMMUNITY, true)) {
                    clazz = Community.class;
                    lastClassWorked = clazz;
                    getCodeFunction = AddressObjects.Object::getCODE;
                    predicate = o -> o.getAOLEVEL().equals(AddressLevel.COMMUNITY.getAddressLevel()) && (testParent(o.getPARENTGUID(), cityId) || testParent(o.getPARENTGUID(), regionId));
                    sendToLog(MessageFormat.format("Start Creating objects of class {0}", clazz.getSimpleName()));
                    progress = getConfigProgress(clazz);
                    percentValue = (int) (Math.abs((double) progress / (double) Files.size(filePath) * 100));
                    taskLifeCycle.publish(percentValue);
                    while (progress <= Files.size(filePath)) {
                        if (taskLifeCycle.isCancelled() || taskLifeCycle.isInterrupted()) {
                            //updateConfigProgress(clazz, progress);
                            break;
                        } else {
                            CommitContext commitContext = new CommitContext();
                            sendToLog("Searching next batch of objects...");
                            List<AddressObjectFork> addressObjectForks = fiasClient.loadList(predicate, filePath, progress, batchSize);
                            if (addressObjectForks.size() == 0) {
                                sendToLog(MessageFormat.format("No new Fias Entities were processed (class = {1}), reached 100% of file",
                                        addressObjectForks.size(),
                                        clazz.getSimpleName()));
                                break;
                            } else {
                                for (AddressObjectFork addressObjectFork : addressObjectForks) {
                                    FiasEntity fe = loadFiasEntity(clazz, addressObjectFork.getObject());
                                    fe.setCode(getCodeFunction.apply(addressObjectFork.getObject()));
                                    commitContext.addInstanceToCommit(fe);
                                    progress = addressObjectFork.getOffset();
                                    percentValue = (int) (Math.abs((double) progress / (double) Files.size(filePath) * 100));
                                    taskLifeCycle.publish(percentValue);
                                }
                                dataManager.commit(commitContext);
                                sendToLog(MessageFormat.format("{0} new Fias Entities were processed (class = {1}), reached {2}% of file",
                                        addressObjectForks.size(),
                                        clazz.getSimpleName(),
                                        percentValue));
                            }
                        }
                    }
                    sendToLog(MessageFormat.format("Finished Creating objects of class {0}", clazz.getSimpleName()));
                }

                //грузим Locations
                if ((boolean) options.getOrDefault(AddressLevel.LOCATION, true)) {
                    clazz = Location.class;
                    lastClassWorked = clazz;
                    getCodeFunction = AddressObjects.Object::getCODE;
                    predicate = o -> o.getAOLEVEL().equals(AddressLevel.LOCATION.getAddressLevel()) && (testParent(o.getPARENTGUID(), cityId) || testParent(o.getPARENTGUID(), regionId));
                    sendToLog(MessageFormat.format("Start Creating objects of class {0}", clazz.getSimpleName()));
                    progress = getConfigProgress(clazz);
                    percentValue = (int) (Math.abs((double) progress / (double) Files.size(filePath) * 100));
                    taskLifeCycle.publish(percentValue);
                    while (progress <= Files.size(filePath)) {
                        if (taskLifeCycle.isCancelled() || taskLifeCycle.isInterrupted()) {
                            //updateConfigProgress(clazz, progress);
                            break;
                        } else {
                            CommitContext commitContext = new CommitContext();
                            sendToLog("Searching next batch of objects...");
                            List<AddressObjectFork> addressObjectForks = fiasClient.loadList(predicate, filePath, progress, batchSize);
                            if (addressObjectForks.size() == 0) {
                                sendToLog(MessageFormat.format("No new Fias Entities were processed (class = {1}), reached 100% of file",
                                        addressObjectForks.size(),
                                        clazz.getSimpleName()));
                                break;
                            } else {
                                for (AddressObjectFork addressObjectFork : addressObjectForks) {
                                    FiasEntity fe = loadFiasEntity(clazz, addressObjectFork.getObject());
                                    fe.setCode(getCodeFunction.apply(addressObjectFork.getObject()));
                                    commitContext.addInstanceToCommit(fe);
                                    progress = addressObjectFork.getOffset();
                                    percentValue = (int) (Math.abs((double) progress / (double) Files.size(filePath) * 100));
                                    taskLifeCycle.publish(percentValue);
                                }
                                dataManager.commit(commitContext);
                                sendToLog(MessageFormat.format("{0} new Fias Entities were processed (class = {1}), reached {2}% of file",
                                        addressObjectForks.size(),
                                        clazz.getSimpleName(),
                                        percentValue));
                            }
                        }
                    }
                    sendToLog(MessageFormat.format("Finished Creating objects of class {0}", clazz.getSimpleName()));
                }

                //грузим Streets
                if ((boolean) options.getOrDefault(AddressLevel.STREET, true)) {
                    clazz = Street.class;
                    lastClassWorked = clazz;
                    getCodeFunction = AddressObjects.Object::getSTREETCODE;
                    predicate = o -> o.getAOLEVEL().equals(AddressLevel.STREET.getAddressLevel()) && (testParent(o.getPARENTGUID(), cityId) || testParent(o.getPARENTGUID(), regionId));
                    sendToLog(MessageFormat.format("Start Creating objects of class {0}", clazz.getSimpleName()));
                    progress = getConfigProgress(clazz);
                    percentValue = (int) (Math.abs((double) progress / (double) Files.size(filePath) * 100));
                    taskLifeCycle.publish(percentValue);
                    while (progress <= Files.size(filePath)) {
                        if (taskLifeCycle.isCancelled() || taskLifeCycle.isInterrupted()) {
                            //updateConfigProgress(clazz, progress);
                            break;
                        } else {
                            CommitContext commitContext = new CommitContext();
                            sendToLog("Searching next batch of objects...");
                            List<AddressObjectFork> addressObjectForks = fiasClient.loadList(predicate, filePath, progress, batchSize);
                            if (addressObjectForks.size() == 0) {
                                sendToLog(MessageFormat.format("No new Fias Entities were processed (class = {1}), reached 100% of file",
                                        addressObjectForks.size(),
                                        clazz.getSimpleName()));
                                break;
                            } else {
                                for (AddressObjectFork addressObjectFork : addressObjectForks) {
                                    FiasEntity fe = loadFiasEntity(clazz, addressObjectFork.getObject());
                                    fe.setCode(getCodeFunction.apply(addressObjectFork.getObject()));
                                    commitContext.addInstanceToCommit(fe);
                                    progress = addressObjectFork.getOffset();
                                    percentValue = (int) (Math.abs((double) progress / (double) Files.size(filePath) * 100));
                                    taskLifeCycle.publish(percentValue);
                                }
                                dataManager.commit(commitContext);
                                sendToLog(MessageFormat.format("{0} new Fias Entities were processed (class = {1}), reached {2}% of file",
                                        addressObjectForks.size(),
                                        clazz.getSimpleName(),
                                        percentValue));
                            }
                        }
                    }
                    sendToLog(MessageFormat.format("Finished Creating objects of class {0}", clazz.getSimpleName()));
                }

                //грузим Houses
                if ((boolean) options.getOrDefault("needLoadHouses", true)) {
                    //Class<House> clazz = House.class;
                    clazz = House.class;
                    lastClassWorked = clazz;
                    Path filePathHouses = getPathByPattern(HOUSE.getName());
                    progress = getConfigProgress(clazz);
                    percentValue = (int) (Math.abs((double) progress / (double) Files.size(filePath) * 100));
                    taskLifeCycle.publish(percentValue);
                    PartialUnmarshallerFork<Houses.House> pum = fiasClient.getUnmarshallerFork(Houses.House.class, progress);
                    sendToLog(MessageFormat.format("Start Creating objects of class {0}", clazz.getSimpleName()));
                    List<House> houses = new ArrayList<>();
                    while (pum.hasNext()) {
                        if (taskLifeCycle.isCancelled() || taskLifeCycle.isInterrupted()) {
                            break;
                        } else {
                            final Houses.House fiasHouse = pum.next();
                            House house = getHouseEntity(fiasHouse);
                            house = processHouseEntity(fiasHouse, house);
                            if (house != null)
                                houses.add(house);
                        }
                        progress = pum.getInputStream().getProgress();
                        percentValue = (int) (Math.abs((double) progress / (double) Files.size(filePathHouses) * 100));
                        taskLifeCycle.publish(percentValue);
                        if (houses.size() == batchSize) {
                            CommitContext commitContext = new CommitContext(houses);
                            dataManager.commit(commitContext);
                            log.debug(MessageFormat.format("{0} new Houses were processed (class = {1}), reached {2}% of file",
                                    houses.size(),
                                    clazz.getSimpleName(),
                                    percentValue));
                            houses.clear();
                        }
                    }
                    sendToLog(MessageFormat.format("Finished Creating objects of class {0}", clazz.getSimpleName()));
                }

                return null;
            }

            @Override
            public void done(Void result) {
                showNotification(getMessage("loadDone"));
                updateConfigProgress(clazz, 0);
                progressBar.setValue(1f);
                progressLabel.setValue("100%");
                setupControlButtons(true);
                super.done(result);
            }

            @Override
            public void progress(List<Integer> changes) {
                progressBar.setValue((changes.get(changes.size() - 1) / 100f));
                progressLabel.setValue(changes.get(changes.size() - 1) + " %");
            }

            @Override
            public void canceled() {
                updateConfigProgress(clazz, progress);
                setupControlButtons(true);
                sendToLog("Task was cancelled");
                showNotification(getMessage("loadCanceled"));
            }
        };
    }

    public void onPauseLoadingDataBtnClick() {
        taskHandler.cancel();
    }

    private long getConfigProgress(Class clazz) {
        long progress = 0;
        switch (clazz.getSimpleName()) {
            case "Area":
                progress = configuration.getConfig(FiasServiceConfig.class).getProgressArea();
                break;
            case "Autonomy":
                progress = configuration.getConfig(FiasServiceConfig.class).getProgressAutonomies();
                break;
            case "City":
                progress = configuration.getConfig(FiasServiceConfig.class).getProgressCity();
                break;
            case "Community":
                progress = configuration.getConfig(FiasServiceConfig.class).getProgressCommunity();
                break;
            case "House":
                progress = configuration.getConfig(FiasServiceConfig.class).getProgressHouses();
                break;
            case "Location":
                progress = configuration.getConfig(FiasServiceConfig.class).getProgressLocation();
                break;
            case "Region":
                progress = configuration.getConfig(FiasServiceConfig.class).getProgressRegions();
                break;
            case "Street":
                progress = configuration.getConfig(FiasServiceConfig.class).getProgressStreet();
                break;
        }
        return progress;
    }

    private void updateConfigProgress(Class clazz, long progress) {
        switch (clazz.getSimpleName()) {
            case "Area":
                configuration.getConfig(FiasServiceConfig.class).setProgressArea(progress);
                break;
            case "Autonomy":
                configuration.getConfig(FiasServiceConfig.class).setProgressAutonomies(progress);
                break;
            case "City":
                configuration.getConfig(FiasServiceConfig.class).setProgressCity(progress);
                break;
            case "Community":
                configuration.getConfig(FiasServiceConfig.class).setProgressCommunity(progress);
                break;
            case "House":
                configuration.getConfig(FiasServiceConfig.class).setProgressHouses(progress);
                break;
            case "Location":
                configuration.getConfig(FiasServiceConfig.class).setProgressLocation(progress);
                break;
            case "Region":
                configuration.getConfig(FiasServiceConfig.class).setProgressRegions(progress);
                break;
            case "Street":
                configuration.getConfig(FiasServiceConfig.class).setProgressStreet(progress);
                break;
        }
    }

    private void resetConfigProgress() {
        configuration.getConfig(FiasServiceConfig.class).setProgressArea(0);
        configuration.getConfig(FiasServiceConfig.class).setProgressAutonomies(0);
        configuration.getConfig(FiasServiceConfig.class).setProgressCity(0);
        configuration.getConfig(FiasServiceConfig.class).setProgressCommunity(0);
        configuration.getConfig(FiasServiceConfig.class).setProgressHouses(0);
        configuration.getConfig(FiasServiceConfig.class).setProgressLocation(0);
        configuration.getConfig(FiasServiceConfig.class).setProgressRegions(0);
        configuration.getConfig(FiasServiceConfig.class).setProgressStreet(0);
    }

    private House getHouseEntity(Houses.House fiasHouse) {
        final UUID entityId;
        final String houseguid = fiasHouse.getHOUSEGUID();
        try {
            entityId = UUID.fromString(houseguid);
        } catch (IllegalArgumentException e) {
            log.warn("Wrong entity ID format (HOUSEGUID) for element {} with id: {}",
                    Houses.House.class.getSimpleName(), houseguid);
            return null;
        }

        return dataManager.load(House.class)
                .id(entityId)
                .view("parent")
                .optional()
                .orElseGet(() -> {
                    final House newEntity = dataManager.create(House.class);
                    newEntity.setId(entityId);
                    return newEntity;
                });
    }

    private House processHouseEntity(Houses.House fiasHouse, House entity) {

        final String aoguid = fiasHouse.getAOGUID();
        if (Strings.isNullOrEmpty(aoguid)) {
            log.warn("Missing parent ID (AOGUID) for element {} with id: {}",
                    Houses.House.class.getSimpleName(), fiasHouse.getHOUSEGUID());
            return null;
        }
        final UUID parentId;
        try {
            parentId = UUID.fromString(aoguid);
        } catch (IllegalArgumentException e) {
            log.warn("Wrong parent ID format (AOGUID) for element {} with id: {}",
                    Houses.House.class.getSimpleName(), fiasHouse.getHOUSEGUID());
            return null;
        }

        final FiasEntity parentEntity = dataManager.load(FiasEntity.class)
                .id(parentId)
                .optional()
                .orElse(null);
        if (parentEntity != null) {
            House house = entity;
            house.setValue("parent", parentEntity, true);

            house.setValue("postalcode", fiasHouse.getPOSTALCODE(), true);
            house.setValue("ifnsfl", fiasHouse.getIFNSFL(), true);
            house.setValue("terrifnsfl", fiasHouse.getTERRIFNSFL(), true);
            house.setValue("ifnsul", fiasHouse.getIFNSUL(), true);
            house.setValue("terrifnsul", fiasHouse.getTERRIFNSUL(), true);
            house.setValue("okato", fiasHouse.getOKATO(), true);
            house.setValue("oktmo", fiasHouse.getOKTMO(), true);
            house.setValue("housenum", fiasHouse.getHOUSENUM(), true);
            house.setValue("eststatus", fiasHouse.getESTSTATUS().intValue(), true);
            house.setValue("buildnum", fiasHouse.getBUILDNUM(), true);
            house.setValue("strstatus", fiasHouse.getSTRSTATUS().intValue(), true);
            house.setValue("strucnum", fiasHouse.getSTRUCNUM(), true);
            house.setValue("startdate", fiasHouse.getSTARTDATE().toGregorianCalendar().getTime(), true);
            house.setValue("enddate", fiasHouse.getENDDATE().toGregorianCalendar().getTime(), true);

            return house;
        } else {
            log.warn("Was unable to find parent (id={}) for element {} with id={}"
                    , fiasHouse.getAOGUID(), Houses.House.class.getSimpleName(), fiasHouse.getHOUSEGUID());
            return null;
        }
    }


    private boolean testParent(String parentguid, UUID requiredId) {
        if (requiredId == null) return true;
        if (parentguid == null) return false;
        UUID parentId;
        try {
            parentId = UUID.fromString(parentguid);
        } catch (IllegalArgumentException e) {
            log.warn("Wrong parentguid format. Value: {}", parentguid);
            return false;
        }
        if (parentId.equals(requiredId))
            return true;
        else {
            Optional<FiasEntity> entity = dataManager.load(FiasEntity.class)
                    .view("parent")
                    .id(parentId)
                    .optional();
            if (entity.isPresent()) {
                FiasEntity parent = entity.get().getParent();
                if (parent != null)
                    return testParent(parent.getId().toString(), requiredId);
            }
        }
        return false;
    }

    private <T extends FiasEntity> FiasEntity loadFiasEntity(Class<T> clazz, AddressObjects.Object object) {
        boolean isRegionObject = AddressLevel.REGION.getAddressLevel().equals(object.getAOLEVEL());
        if (object.getPARENTGUID() == null && !isRegionObject) {
            log.warn("Missing parent ID (PARENTGUID) for element id={}, name={}", object.getAOGUID(), object.getOFFNAME());
            return null;
        }
        UUID id = UUID.fromString(object.getAOGUID());
        FiasEntity entity = dataManager.load(FiasEntity.class)
                .id(id)
                .view("parent")
                .optional()
                .orElseGet(() -> {
                    T newEntity = dataManager.create(clazz);
                    newEntity.setId(id);
                    return newEntity;
                });
        //entity = persistence.getEntityManager().merge(entity);
        UUID parentId;
        FiasEntity parent = null;
        if (object.getPARENTGUID() != null) {
            parentId = UUID.fromString(object.getPARENTGUID());
            parent = dataManager.load(FiasEntity.class)
                    .id(parentId)
                    .optional()
                    .orElse(null);
        }
        if (parent == null && !isRegionObject)
            return null;

        entity.setValue("name", object.getOFFNAME(), true);
        entity.setValue("offname", object.getOFFNAME(), true);
        entity.setValue("shortname", object.getSHORTNAME(), true);
        entity.setValue("formalname", object.getFORMALNAME(), true);
        entity.setValue("postalCode", object.getPOSTALCODE(), true);
        entity.setValue("parent", parent, true);
        List<String> names = Lists.newArrayList(object.getFORMALNAME(), object.getOFFNAME());
        entity.setValue("possibleNames", String.join(",", names), true);

        entity.setValue("updatedate", object.getUPDATEDATE().toGregorianCalendar().getTime(), true);
        entity.setValue("actstatus", FiasEntityStatus.fromId(object.getACTSTATUS().intValue()), true);
        entity.setValue("operstatus", FiasEntityOperationStatus.fromId(object.getOPERSTATUS().intValue()), true);
        entity.setValue("startdate", object.getSTARTDATE().toGregorianCalendar().getTime(), true);
        entity.setValue("enddate", object.getENDDATE().toGregorianCalendar().getTime(), true);
        return entity;
    }

    public Map<Class, FiasEntity> getAddressComponents(House house) {
        if (!PersistenceHelper.isLoaded(house, "parent"))
            house = dataManager.reload(house, "parent");

        final FiasEntity fiasEntity = house.getParent();

        final HashMap<Class, FiasEntity> entityMap = new HashMap<>();
        findFiasEntityParent(fiasEntity, entityMap);

        return entityMap;
    }

    public Map<Class, FiasEntity> getAddressComponents(UUID houseId) {
        final Optional<House> houseOptional = dataManager.load(House.class)
                .id(houseId)
                .view("parent")
                .optional();
        return houseOptional.map(this::getAddressComponents).orElse(null);
    }

    private void findFiasEntityParent(FiasEntity fiasEntity, HashMap<Class, FiasEntity> entityMap) {
        if (!PersistenceHelper.isLoaded(fiasEntity, "parent"))
            fiasEntity = dataManager.reload(fiasEntity, "parent");
        entityMap.put(fiasEntity.getClass(), fiasEntity);
        if (fiasEntity.getParent() != null) {
            findFiasEntityParent(fiasEntity.getParent(), entityMap);
        }
    }

    private Path getPathByPattern(String startsWith) throws IOException {
        Optional<Path> filePath = Files.list(xmlDirectory)
                .map(xmlDirectory::relativize)
                .filter(path -> path.toString().startsWith(startsWith) && path.toString().toLowerCase().endsWith("xml"))
                .findFirst();
        if (!filePath.isPresent()) {
            throw new FileNotFoundException(String.format("Can't find XML file with name starting with [%s]", startsWith));
        }
        return xmlDirectory.resolve(filePath.get());
    }

    public void onResetProgressButtonClick() {
        resetConfigProgress();
        log.warn("Progress was reseted");
    }

    //слушатели на закрытие окна (для сохранения прогресса)
    private void setupCloseWindowListeners() {
        //слушатель на закрытие окна по кнопке крестик
        this.addBeforeCloseWithCloseButtonListener(e -> {
            if (taskWasStarted && lastClassWorked != null) {
                taskHandler.cancel();
                updateConfigProgress(lastClassWorked, progress);
            }
        });
        //слушатель на закрытие окна по shortcut Esc
        this.addBeforeCloseWithShortcutListener(e -> {
            if (taskWasStarted && lastClassWorked != null) {
                taskHandler.cancel();
                updateConfigProgress(lastClassWorked, progress);
            }
        });
    }

    //обновляем доступность кнопок
    private void setupControlButtons(boolean taskIsStopped) {
        if (taskIsStopped) {
            loadDataBtn.setEnabled(true);
            resetProgressButton.setEnabled(true);
            pauseLoadingDataBtn.setEnabled(false);
        } else {
            loadDataBtn.setEnabled(false);
            resetProgressButton.setEnabled(false);
            pauseLoadingDataBtn.setEnabled(true);
        }
    }

    private void sendToLog(String text) {
        log.info(text);
    }
}