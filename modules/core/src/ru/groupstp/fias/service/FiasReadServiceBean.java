package ru.groupstp.fias.service;

import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.Metadata;
import org.meridor.fias.AddressObjects;
import org.meridor.fias.Fias;
import org.meridor.fias.FiasClient;
import org.meridor.fias.enums.AddressLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.groupstp.fias.entity.*;
import sun.rmi.runtime.Log;

import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.NoResultException;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@Service(FiasReadService.NAME)
public class FiasReadServiceBean implements FiasReadService {

    private static final Logger log = LoggerFactory.getLogger(FiasReadService.class);

    @Inject
    private DataManager dataManager;

    FiasClient fiasClient;

    @Override
    public void readFias() throws FileNotFoundException {
        Path xmlDirectory = Paths.get("/mnt/sda2/lobo/fias/xml");
        fiasClient= new FiasClient(xmlDirectory);

        loadObjects(AddressLevel.REGION, Region.class,o -> o.getREGIONCODE());
        loadObjects(AddressLevel.AUTONOMY, Autonomy.class,  o -> o.getCODE());
        loadObjects(AddressLevel.AREA, Area.class, o -> o.getAREACODE());
        loadObjects(AddressLevel.CITY, City.class, o -> o.getCITYCODE());
        loadObjects(AddressLevel.COMMUNITY, Community.class, o -> o.getCODE());
        loadObjects(AddressLevel.LOCATION, Location.class, o -> o.getCODE());
        loadObjects(AddressLevel.STREET, Street.class, o -> o.getSTREETCODE());

        //TODO: loadHouses
        //loadHouses();
    }



    void loadObjects(AddressLevel level, Class clazz, Function<AddressObjects.Object, String> getCodeFunction)
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
            dataManager.commit(entity);
        });
    }

    FiasEntity loadFiasEntity(Class clazz, AddressObjects.Object object)
    {
        UUID id = UUID.fromString(object.getAOGUID());
        FiasEntity entity = null;
        try {
             entity = (FiasEntity) dataManager.load(clazz).id(id).view("parent").one();
        }
        catch (IllegalStateException e)
        {
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
                    UUID parentId = UUID.fromString(object.getPARENTGUID());
                    FiasEntity parent = dataManager.load(FiasEntity.class).id(parentId).one();
                    entity.setParent(parent);
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
            return entity;
        }
    }

    private static HashMap <String, StandardEntity> stringEntityHashMap = new HashMap<>();

    @Inject
    private Metadata metadata;

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