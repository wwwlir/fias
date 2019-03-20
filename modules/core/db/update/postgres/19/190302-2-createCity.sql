alter table FIAS_CITY add constraint FK_FIAS_CITY_ON_REGION foreign key (REGION_ID) references FIAS_REGION(ID);
create index IDX_FIAS_CITY_ON_REGION on FIAS_CITY (REGION_ID);
