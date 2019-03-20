alter table FIAS_AREA add column POSSIBLE_NAMES text ;
alter table FIAS_AREA add column OFFNAME varchar(255) ;
alter table FIAS_AREA add column FORMALNAME varchar(255) ;
alter table FIAS_AREA add column SHORTNAME varchar(50) ;
alter table FIAS_AREA add column REGION_ID uuid ;
alter table FIAS_AREA alter column CODE set data type varchar(3) ;
