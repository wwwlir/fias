package com.groupstp.fias.web.screens;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.groupstp.fias.client.AddressObjectFork;
import com.groupstp.fias.client.FiasClientFork;
import com.groupstp.fias.client.ProgressCounterFilterInputStream;
import com.groupstp.fias.config.FiasServiceConfig;
import com.groupstp.fias.entity.*;
import com.groupstp.fias.entity.enums.FiasEntityOperationStatus;
import com.groupstp.fias.entity.enums.FiasEntityStatus;
import com.groupstp.fias.service.FiasReadService;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.components.AbstractWindow;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.components.ProgressBar;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundTaskHandler;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import org.meridor.fias.AddressObjects;
import org.meridor.fias.Houses;
import org.meridor.fias.enums.AddressLevel;
import org.meridor.fias.loader.PartialUnmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.meridor.fias.enums.FiasFile.ADDRESS_OBJECTS;

public class Fiasclientv2 extends AbstractWindow {
    private static final Logger log = LoggerFactory.getLogger(FiasReadService.class);

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

    @Inject
    private DataManager dataManager;
    @Inject
    private Configuration configuration;
//    @Inject
//    private Persistence persistence;

    //private FiasClient fiasClient;
    private FiasClientFork fiasClient;
    private Path xmlDirectory;

    private long progress;

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

        UUID regionId = ((UUID) options.getOrDefault("regionId", null));
        UUID cityId = ((UUID) options.getOrDefault("cityId", null));

        progressBar.setIndeterminate(true);

        if ((boolean) options.getOrDefault(AddressLevel.REGION, true)) {
            taskHandler = backgroundWorker.handle(createBackgroundTask(options,
                    Region.class,
                    AddressObjects.Object::getREGIONCODE,
                    o -> o.getAOLEVEL().equals(AddressLevel.REGION.getAddressLevel())));
            taskHandler.execute();
        }
        if ((boolean) options.getOrDefault(AddressLevel.AUTONOMY, true)) {
            taskHandler = backgroundWorker.handle(createBackgroundTask(options,
                    Autonomy.class,
                    AddressObjects.Object::getCODE,
                    o -> o.getAOLEVEL().equals(AddressLevel.AUTONOMY.getAddressLevel()) && testParent(o.getPARENTGUID(), regionId)));
            taskHandler.execute();
        }
        if ((boolean) options.getOrDefault(AddressLevel.AREA, true)) {
            taskHandler = backgroundWorker.handle(createBackgroundTask(options,
                    Area.class,
                    AddressObjects.Object::getAREACODE,
                    o -> o.getAOLEVEL().equals(AddressLevel.AREA.getAddressLevel()) && testParent(o.getPARENTGUID(), regionId)));
            taskHandler.execute();
        }
        if ((boolean) options.getOrDefault(AddressLevel.CITY, true)) {
            taskHandler = backgroundWorker.handle(createBackgroundTask(options,
                    City.class,
                    AddressObjects.Object::getCITYCODE,
                    o -> o.getAOLEVEL().equals(AddressLevel.CITY.getAddressLevel()) && testParent(o.getPARENTGUID(), regionId)));
            taskHandler.execute();
        }
        if ((boolean) options.getOrDefault(AddressLevel.COMMUNITY, true)) {
            taskHandler = backgroundWorker.handle(createBackgroundTask(options,
                    Community.class,
                    AddressObjects.Object::getCODE,
                    o -> o.getAOLEVEL().equals(AddressLevel.COMMUNITY.getAddressLevel()) && (testParent(o.getPARENTGUID(), cityId) || testParent(o.getPARENTGUID(), regionId))));
            taskHandler.execute();
        }
        if ((boolean) options.getOrDefault(AddressLevel.LOCATION, true)) {
            taskHandler = backgroundWorker.handle(createBackgroundTask(options,
                    Location.class,
                    AddressObjects.Object::getCODE,
                    o -> o.getAOLEVEL().equals(AddressLevel.LOCATION.getAddressLevel()) && (testParent(o.getPARENTGUID(), cityId) || testParent(o.getPARENTGUID(), regionId))));
            taskHandler.execute();
        }
        if ((boolean) options.getOrDefault(AddressLevel.STREET, true)) {
            taskHandler = backgroundWorker.handle(createBackgroundTask(options,
                    Street.class,
                    AddressObjects.Object::getSTREETCODE,
                    o -> o.getAOLEVEL().equals(AddressLevel.STREET.getAddressLevel()) && (testParent(o.getPARENTGUID(), cityId) || testParent(o.getPARENTGUID(), regionId))));
            taskHandler.execute();
        }
        //if ((boolean) options.getOrDefault("needLoadHouses", true))
        //loadHouses();
    }

    private BackgroundTask<Integer, Void> createBackgroundTask(Map<Object, Object> options, Class<? extends FiasEntity> clazz, Function<AddressObjects.Object, String> getCodeFunction, Predicate<AddressObjects.Object> predicate) {
        return new BackgroundTask<Integer, Void>(TimeUnit.HOURS.toSeconds(5), this) {
            int percentValue = 1;
            @Override
            public Void run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {
                String path = configuration.getConfig(FiasServiceConfig.class).getPath();
                int batchSize = configuration.getConfig(FiasServiceConfig.class).getBatchSize();
                xmlDirectory = Paths.get(path);
                fiasClient = new FiasClientFork(xmlDirectory);

                log.debug("Creating objects of class {}", clazz.getSimpleName());
                Path filePath = getPathByPattern(ADDRESS_OBJECTS.getName());
                progress = getConfigProgress(clazz);
                while (progress <= Files.size(filePath)) {
                    if (taskLifeCycle.isCancelled() || taskLifeCycle.isInterrupted()) {
                        //updateConfigProgress(clazz, progress);
                        break;
                    } else {
//                        AddressObjectFork addressObjectFork = fiasClient.load(predicate, filePath, progress);
//                        FiasEntity fe = loadFiasEntity(clazz, addressObjectFork.getObject());
//                        dataManager.commit(fe);
//                        log.debug("New Entity {} was created (class = {}), reached {}% of file",
//                                fe.getUuid(),
//                                clazz.getSimpleName(),
//                                (int) (Math.abs((double) addressObjectFork.getOffset() / (double) Files.size(filePath) * 100)));
//                        progress = addressObjectFork.getOffset();

                        CommitContext commitContext = new CommitContext();
                        List<AddressObjectFork> addressObjectForks = fiasClient.loadList(predicate, filePath, progress, batchSize);
                        for (AddressObjectFork addressObjectFork : addressObjectForks) {
                            FiasEntity fe = loadFiasEntity(clazz, addressObjectFork.getObject());
                            commitContext.addInstanceToCommit(fe);
                            progress = addressObjectFork.getOffset();
                        }
                        dataManager.commit(commitContext);
                        percentValue = (int) (Math.abs((double) progress / (double) Files.size(filePath) * 100));
                        log.debug("{} new Fias Entities were processed (class = {}), reached {}% of file",
                                addressObjectForks.size(),
                                clazz.getSimpleName(),
                                percentValue);
                    }
                }
                return null;
            }

            @Override
            public void done(Void result) {
                showNotification(getMessage("loadDone"));
                updateConfigProgress(clazz, 0);
                progressBar.setIndeterminate(false);
                super.done(result);
            }

            @Override
            public void progress(List<Integer> changes) {
                progressBar.setIndeterminate(false);
//                log.debug(" (class = {}), reached {}% of file",
//                        clazz.getSimpleName(),
//                        percentValue);
                super.progress(changes);
            }

            @Override
            public void canceled() {
                progressBar.setIndeterminate(false);
                updateConfigProgress(clazz, progress);
                showNotification("Задача была отменена");
            }
        };
    }

    public void onPauseLoadingDataBtnClick() {
        //if (backgroundWorker.handle(task).isAlive())
        taskHandler.cancel();
    }

    private long getConfigProgress(Class<? extends FiasEntity> clazz) {
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

    private void updateConfigProgress(Class<? extends FiasEntity> clazz, long progress) {
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

    private void loadHouses() throws FileNotFoundException {
        FiasClientFork housesClient = new FiasClientFork(xmlDirectory);
        PartialUnmarshaller<Houses.House> pum = housesClient.getUnmarshaller(Houses.House.class);
        while (pum.hasNext()) {
            final Houses.House fiasHouse = pum.next();
            final House house = getHouseEntity(fiasHouse);
            //processHouseEntity(fiasHouse, house);
        }
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

//    private void processHouseEntity(Houses.House fiasHouse, House entity) {
//        persistence.runInTransaction(em -> {
//            final String aoguid = fiasHouse.getAOGUID();
//            if (Strings.isNullOrEmpty(aoguid)) {
//                log.warn("Missing parent ID (AOGUID) for element {} with id: {}",
//                        Houses.House.class.getSimpleName(), fiasHouse.getHOUSEGUID());
//                return;
//            }
//            final UUID parentId;
//            try {
//                parentId = UUID.fromString(aoguid);
//            } catch (IllegalArgumentException e) {
//                log.warn("Wrong parent ID format (AOGUID) for element {} with id: {}",
//                        Houses.House.class.getSimpleName(), fiasHouse.getHOUSEGUID());
//                return;
//            }
//
//            final FiasEntity parentEntity = dataManager.load(FiasEntity.class)
//                    .id(parentId)
//                    .optional()
//                    .orElse(null);
//            if (parentEntity != null) {
//                House house = em.merge(entity);
//                house.setValue("parent", parentEntity, true);
//
//                house.setValue("postalcode", fiasHouse.getPOSTALCODE(), true);
//                house.setValue("ifnsfl", fiasHouse.getIFNSFL(), true);
//                house.setValue("terrifnsfl", fiasHouse.getTERRIFNSFL(), true);
//                house.setValue("ifnsul", fiasHouse.getIFNSUL(), true);
//                house.setValue("terrifnsul", fiasHouse.getTERRIFNSUL(), true);
//                house.setValue("okato", fiasHouse.getOKATO(), true);
//                house.setValue("oktmo", fiasHouse.getOKTMO(), true);
//                house.setValue("housenum", fiasHouse.getHOUSENUM(), true);
//                house.setValue("eststatus", fiasHouse.getESTSTATUS().intValue(), true);
//                house.setValue("buildnum", fiasHouse.getBUILDNUM(), true);
//                house.setValue("strstatus", fiasHouse.getSTRSTATUS().intValue(), true);
//                house.setValue("strucnum", fiasHouse.getSTRUCNUM(), true);
//                house.setValue("startdate", fiasHouse.getSTARTDATE().toGregorianCalendar().getTime(), true);
//                house.setValue("enddate", fiasHouse.getENDDATE().toGregorianCalendar().getTime(), true);
//
//            } else {
//                log.warn("Was unable to find parent (id={}) for element {} with id={}"
//                        , fiasHouse.getAOGUID(), Houses.House.class.getSimpleName(), fiasHouse.getHOUSEGUID());
//            }
//        });
//    }

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
}