alter table FIAS_REGION alter column CODE set data type varchar(13) ;
-- update FIAS_REGION set CODE = <default_value> where CODE is null ;
alter table FIAS_REGION alter column CODE set not null ;
