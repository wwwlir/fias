package ru.groupstp.fias.service;

import com.google.common.base.Strings;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.PersistenceHelper;
import org.meridor.fias.AddressObjects;
import org.meridor.fias.FiasClient;
import org.meridor.fias.Houses;
import org.meridor.fias.enums.AddressLevel;
import org.meridor.fias.loader.PartialUnmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.groupstp.fias.client.FiasClientFork;
import ru.groupstp.fias.config.FiasServiceConfig;
import ru.groupstp.fias.entity.FiasEntity;
import ru.groupstp.fias.entity.House;

import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

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

        String path = configuration.getConfig(FiasServiceConfig.class).getPath();
        xmlDirectory = Paths.get(path);

        fiasClient= new FiasClient(xmlDirectory);

//        loadObjects(AddressLevel.REGION, Region.class, AddressObjects.Object::getREGIONCODE);
//        loadObjects(AddressLevel.AUTONOMY, Autonomy.class, AddressObjects.Object::getCODE);
//        loadObjects(AddressLevel.AREA, Area.class, AddressObjects.Object::getAREACODE);
//        loadObjects(AddressLevel.CITY, City.class, AddressObjects.Object::getCITYCODE);
//        loadObjects(AddressLevel.COMMUNITY, Community.class, AddressObjects.Object::getCODE);
//        loadObjects(AddressLevel.LOCATION, Location.class, AddressObjects.Object::getCODE);
//        loadObjects(AddressLevel.STREET, Street.class, AddressObjects.Object::getSTREETCODE);
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

    private void processHouseEntity(Houses.House fiasHouse, House house) {
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

        final Optional<FiasEntity> parentOptional = dataManager.load(FiasEntity.class)
                .id(parentId).optional();
        final FiasEntity parentEntity = parentOptional.orElse(null);
        if (parentEntity != null){
            house.setValue("parent", parentEntity, true);
            house.setValue("eststatus", fiasHouse.getESTSTATUS().intValue(), true);
            house.setValue("buildnum", fiasHouse.getBUILDNUM(), true);
            house.setValue("strstatus", fiasHouse.getSTRSTATUS().intValue(), true);
            house.setValue("strucnum", fiasHouse.getSTRUCNUM(), true);
            house.setValue("startdate", fiasHouse.getSTARTDATE().toGregorianCalendar().getTime(), true);
            house.setValue("enddate", fiasHouse.getENDDATE().toGregorianCalendar().getTime(), true);
            if (persistence.getTools().isDirty(house) || PersistenceHelper.isNew(house))
                dataManager.commit(house);
        } else {
            log.warn("Was unable to find parent (id={}) for element {} with id={}"
                    , fiasHouse.getAOGUID(), Houses.House.class.getSimpleName(), fiasHouse.getHOUSEGUID());
        }
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

        final Optional<House> optionalHouse = dataManager.load(House.class)
                .id(entityId)
                .view("parent")
                .optional();
        return optionalHouse.orElseGet(() -> {
            final House newEntity = dataManager.create(House.class);
            newEntity.setId(entityId);
            return newEntity;
        });
    }


    private void loadObjects(AddressLevel level, Class clazz, Function<AddressObjects.Object, String> getCodeFunction)
    {
        log.debug("Loading objects of level {}", level.name());
        List<AddressObjects.Object> objects = fiasClient.load(o -> o.getAOLEVEL().equals(level.getAddressLevel()));
//        if(objects.size()>0)
//            loader.accept(objects.get(0));
        objects.forEach(object -> {
            FiasEntity entity = loadFiasEntity(clazz, object);
            if(entity==null)
                return;
            entity.setCode(getCodeFunction.apply(object));
            try {
                FiasEntity commit = dataManager.commit(entity);
            }
            catch (Exception e)
            {
                log.error("Error while commit {}: {}, {}", clazz.getSimpleName(), entity.getName(), e.getMessage());
            }
        });
    }

    private FiasEntity loadFiasEntity(Class clazz, AddressObjects.Object object)
    {
        UUID id = UUID.fromString(object.getAOGUID());
        FiasEntity entity = null;
        try {
            entity = (FiasEntity) dataManager.load(clazz).id(id).view("parent").one();
        }
        catch (IllegalStateException e) {
            entity = (FiasEntity) dataManager.create(clazz);
        }
        finally {
            entity.setId(id);
            entity.setValue("name", object.getOFFNAME());
            entity.setValue("offname", object.getOFFNAME());
            entity.setValue("shortname", object.getSHORTNAME());
            entity.setValue("formalname", object.getFORMALNAME());
            entity.setPostalCode(object.getPOSTALCODE());
            try {
                if(object.getPARENTGUID()!=null) {
                    entity.setParent(get(object.getPARENTGUID()));
                }
            }
            catch (Exception y)
            {
                log.error("Error while loading {}: {}, {}", clazz.getSimpleName(), entity.getName(), y.getMessage());
                return null;
            }

            List<String> names = new ArrayList<>();
            names.add(object.getFORMALNAME());
            names.add(object.getOFFNAME());
            entity.setValue("possibleNames", String.join(",", names));
        }
        return entity;
    }

    private static HashMap <String, StandardEntity> stringEntityHashMap = new HashMap<>();

    @Inject
    private Metadata metadata;

    private FiasEntity get(String id)
    {
        if(stringEntityHashMap.containsKey(id))
            return (FiasEntity) stringEntityHashMap.get(id);
        UUID parentId = UUID.fromString(id);
        FiasEntity parent = dataManager.load(FiasEntity.class).id(parentId).one();
        stringEntityHashMap.put(id, parent);
        return parent;
    }

    <T> T getByCode(Class<T> clazz, String code){
        String key = clazz.getName()+code;
        if(stringEntityHashMap.containsKey(key))
            return (T) stringEntityHashMap.get(key);
        T entity = dataManager.loadValue("select e from fias$"+clazz.getSimpleName()+" e where e.code=:code", clazz).
                parameter("code", code).one();

        stringEntityHashMap.put(key,  (StandardEntity) entity);
        return entity;
    }

}