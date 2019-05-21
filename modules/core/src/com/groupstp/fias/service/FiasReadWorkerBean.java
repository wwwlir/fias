package com.groupstp.fias.service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.groupstp.fias.client.FiasClientFork;
import com.groupstp.fias.config.FiasServiceConfig;
import com.groupstp.fias.entity.*;
import com.groupstp.fias.entity.enums.FiasEntityOperationStatus;
import com.groupstp.fias.entity.enums.FiasEntityStatus;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DataManager;
import org.meridor.fias.AddressObjects;
import org.meridor.fias.FiasClient;
import org.meridor.fias.Houses;
import org.meridor.fias.enums.AddressLevel;
import org.meridor.fias.loader.PartialUnmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

@Service(FiasReadService.NAME)
public class FiasReadWorkerBean implements FiasReadService {

    private static final Logger log = LoggerFactory.getLogger(FiasReadService.class);

    @Inject
    private DataManager dataManager;
    @Inject
    private Configuration configuration;
    @Inject
    private Persistence persistence;

    private FiasClient fiasClient;
    private Path xmlDirectory;

    @Override
    public void readFias() throws FileNotFoundException {
        readFias(new HashMap<>());
    }

    @Override
    public void readFias(Map<Object, Object> options) throws FileNotFoundException {
        String path = configuration.getConfig(FiasServiceConfig.class).getPath();
        xmlDirectory = Paths.get(path);
        fiasClient= new FiasClient(xmlDirectory);
        UUID regionId = ((UUID) options.getOrDefault("regionId", null));

        if ((boolean) options.getOrDefault(AddressLevel.REGION, true))
            loadObjects(Region.class, AddressObjects.Object::getREGIONCODE,
                    o -> o.getAOLEVEL().equals(AddressLevel.REGION.getAddressLevel()));
        if ((boolean) options.getOrDefault(AddressLevel.AUTONOMY, true))
            loadObjects(Autonomy.class, AddressObjects.Object::getCODE,
                    o -> o.getAOLEVEL().equals(AddressLevel.AUTONOMY.getAddressLevel()) && testRegion(o.getPARENTGUID(), regionId));
        if ((boolean) options.getOrDefault(AddressLevel.AREA, true))
            loadObjects(Area.class, AddressObjects.Object::getAREACODE,
                    o -> o.getAOLEVEL().equals(AddressLevel.AREA.getAddressLevel()) && testRegion(o.getPARENTGUID(), regionId));
        if ((boolean) options.getOrDefault(AddressLevel.CITY, true))
            loadObjects(City.class, AddressObjects.Object::getCITYCODE,
                    o -> o.getAOLEVEL().equals(AddressLevel.CITY.getAddressLevel()) && testRegion(o.getPARENTGUID(), regionId));
        if ((boolean) options.getOrDefault(AddressLevel.COMMUNITY, true))
            loadObjects(Community.class, AddressObjects.Object::getCODE
                    , o -> o.getAOLEVEL().equals(AddressLevel.COMMUNITY.getAddressLevel()) && testRegion(o.getPARENTGUID(), regionId));
        if ((boolean) options.getOrDefault(AddressLevel.LOCATION, true))
            loadObjects(Location.class, AddressObjects.Object::getCODE
                    , o -> o.getAOLEVEL().equals(AddressLevel.LOCATION.getAddressLevel()) && testRegion(o.getPARENTGUID(), regionId));
        if ((boolean) options.getOrDefault(AddressLevel.STREET, true))
            loadObjects(Street.class, AddressObjects.Object::getSTREETCODE,
                    o -> o.getAOLEVEL().equals(AddressLevel.STREET.getAddressLevel()) && testRegion(o.getPARENTGUID(), regionId));
        if ((boolean) options.getOrDefault("needLoadHouses", true))
            loadHouses();
    }

    private void loadHouses() throws FileNotFoundException {
        FiasClientFork housesClient = new FiasClientFork(xmlDirectory);
        PartialUnmarshaller<Houses.House> pum = housesClient.getUnmarshaller(Houses.House.class);
        while (pum.hasNext()) {
            final Houses.House fiasHouse = pum.next();
            final House house = getHouseEntity(fiasHouse);
            processHouseEntity(fiasHouse, house);
        }
    }

    private void processHouseEntity(Houses.House fiasHouse, House entity) {
        persistence.runInTransaction(em -> {
            final String aoguid = fiasHouse.getAOGUID();
            if (Strings.isNullOrEmpty(aoguid)) {
                log.warn("Missing parent ID (AOGUID) for element {} with id: {}",
                        Houses.House.class.getSimpleName(), fiasHouse.getHOUSEGUID());
                return;
            }
            final UUID parentId;
            try {
                parentId = UUID.fromString(aoguid);
            } catch (IllegalArgumentException e) {
                log.warn("Wrong parent ID format (AOGUID) for element {} with id: {}",
                        Houses.House.class.getSimpleName(), fiasHouse.getHOUSEGUID());
                return;
            }

            final FiasEntity parentEntity = dataManager.load(FiasEntity.class)
                    .id(parentId)
                    .optional()
                    .orElse(null);
            if (parentEntity != null){
                House house = em.merge(entity);
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

            } else {
                log.warn("Was unable to find parent (id={}) for element {} with id={}"
                        , fiasHouse.getAOGUID(), Houses.House.class.getSimpleName(), fiasHouse.getHOUSEGUID());
            }
        });
    }

    private House getHouseEntity(Houses.House fiasHouse) {
        final UUID entityId;
        final String houseguid = fiasHouse.getHOUSEGUID();
        try {
            entityId = UUID.fromString(houseguid);
        } catch(IllegalArgumentException e) {
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


    private void loadObjects(Class<? extends FiasEntity> clazz
            , Function<AddressObjects.Object, String> getCodeFunction
            , Predicate<AddressObjects.Object> predicate)
    {
        log.debug("Loading objects of class {}", clazz.getSimpleName());
        List<AddressObjects.Object> objects = fiasClient.load(predicate);

        objects.forEach(object ->
                persistence.runInTransaction(em -> {
                    FiasEntity entity = loadFiasEntity(clazz, object);
                    if(entity==null)
                        return;
                    entity.setCode(getCodeFunction.apply(object));
                }));
    }

    private boolean testRegion(String parentguid, UUID requiredId) {
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
                if (parent!=null)
                    return testRegion(parent.getId().toString(), requiredId);
            }
        }
        return false;
    }

    private <T extends FiasEntity> FiasEntity loadFiasEntity(Class<T> clazz, AddressObjects.Object object){
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
        entity = persistence.getEntityManager().merge(entity);
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
}