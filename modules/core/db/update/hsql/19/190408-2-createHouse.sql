alter table FIAS_HOUSE add constraint FK_FIAS_HOUSE_ON_PARENT foreign key (PARENT_ID) references FIAS_FIAS_ENTITY(ID);
create index IDX_FIAS_HOUSE_ON_PARENT on FIAS_HOUSE (PARENT_ID);
