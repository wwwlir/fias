package com.groupstp.fias.client;

import org.meridor.fias.AddressObjects;

//Класс-обертка для хранения абсолютной позиции текущего найденного объекта в файле
public class AddressObjectFork {
    private AddressObjects.Object object;
    private long offset;

    public AddressObjects.Object getObject() {
        return object;
    }

    public long getOffset() {
        return offset;
    }

    public AddressObjectFork(AddressObjects.Object object, long offset) {
        this.object = object;
        this.offset = offset;
    }
}
