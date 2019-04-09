-- begin FIAS_FIAS_ENTITY
create table FIAS_FIAS_ENTITY (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    DTYPE varchar(100),
    --
    NAME varchar(255),
    PARENT_ID varchar(36),
    POSTAL_CODE varchar(6),
    OFFNAME varchar(255),
    FORMALNAME varchar(255),
    POSSIBLE_NAMES longvarchar,
    CODE varchar(255),
    SHORTNAME varchar(10),
    --
    primary key (ID)
)^
-- end FIAS_FIAS_ENTITY
-- begin FIAS_HOUSE
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
    POSTALCODE varchar(6),
    IFNSFL varchar(4),
    TERRIFNSFL varchar(4),
    IFNSUL varchar(4),
    TERRIFNSUL varchar(4),
    OKATO varchar(11),
    OKTMO varchar(11),
    HOUSENUM varchar(20),
    ESTSTATUS integer,
    BUILDNUM varchar(10),
    STRUCNUM varchar(10),
    STRSTATUS integer,
    STARTDATE timestamp,
    ENDDATE date,
    PARENT_ID varchar(36) not null,
    --
    primary key (ID)
)^
-- end FIAS_HOUSE
