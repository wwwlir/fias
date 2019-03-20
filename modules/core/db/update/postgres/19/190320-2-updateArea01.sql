alter table FIAS_AREA add constraint FK_FIAS_AREA_ON_REGION foreign key (REGION_ID) references FIAS_REGION(ID);
create index IDX_FIAS_AREA_ON_REGION on FIAS_AREA (REGION_ID);
