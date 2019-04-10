create table FIAS_HOUSE (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    ESTSTATUS integer,
    BUILDNUM varchar(10),
    STRUCNUM varchar(10),
    STRSTATUS integer,
    STARTDATE timestamp,
    ENDDATE date,
    PARENT_ID varchar(36),
    --
    primary key (ID)
);
