alter table FIAS_CITY add constraint FK_FIAS_CITY_ON_AREA foreign key (AREA_ID) references FIAS_AREA(ID);
create index IDX_FIAS_CITY_ON_AREA on FIAS_CITY (AREA_ID);
