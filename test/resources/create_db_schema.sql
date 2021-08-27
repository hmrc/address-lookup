DROP SCHEMA IF EXISTS __schema__ CASCADE;
CREATE SCHEMA __schema__;

--BLPU
CREATE TABLE __schema__.abp_blpu (
                                     RECORD_IDENTIFIER SMALLINT,
                                     CHANGE_TYPE CHARACTER VARYING(1),
                                     PRO_ORDER BIGINT,
                                     UPRN BIGINT,
                                     LOGICAL_STATUS SMALLINT,
                                     BLPU_STATE SMALLINT,
                                     BLPU_STATE_DATE DATE,
                                     PARENT_UPRN BIGINT,
                                     X_COORDINATE DOUBLE PRECISION,
                                     Y_COORDINATE DOUBLE PRECISION,
                                     LATITUDE DOUBLE PRECISION,
                                     LONGITUDE DOUBLE PRECISION,
                                     RPC SMALLINT,
                                     LOCAL_CUSTODIAN_CODE SMALLINT,
                                     COUNTRY CHARACTER VARYING(1),
                                     START_DATE DATE,
                                     END_DATE DATE,
                                     LAST_UPDATE_DATE DATE,
                                     ENTRY_DATE DATE,
                                     ADDRESSBASE_POSTAL CHARACTER VARYING(1),
                                     POSTCODE_LOCATOR CHARACTER VARYING(8),
                                     MULTI_OCC_COUNT SMALLINT
);

--Classification
CREATE TABLE __schema__.abp_classification (
                                               RECORD_IDENTIFIER SMALLINT,
                                               CHANGE_TYPE CHARACTER VARYING(1),
                                               PRO_ORDER BIGINT,
                                               UPRN BIGINT,
                                               CLASS_KEY CHARACTER VARYING(14),
                                               CLASSIFICATION_CODE CHARACTER VARYING(6),
                                               CLASS_SCHEME CHARACTER VARYING(60),
                                               SCHEME_VERSION DOUBLE PRECISION,
                                               START_DATE DATE,
                                               END_DATE DATE,
                                               LAST_UPDATE_DATE DATE,
                                               ENTRY_DATE DATE
);

--Application Cross reference table
CREATE TABLE __schema__.abp_crossref (
                                         RECORD_IDENTIFIER SMALLINT,
                                         CHANGE_TYPE CHARACTER VARYING(1),
                                         PRO_ORDER BIGINT,
                                         UPRN BIGINT,
                                         XREF_KEY CHARACTER VARYING(14),
                                         CROSS_REFERENCE CHARACTER VARYING(50),
                                         VERSION SMALLINT,
                                         SOURCE CHARACTER VARYING(6),
                                         START_DATE DATE,
                                         END_DATE DATE,
                                         LAST_UPDATE_DATE DATE,
                                         ENTRY_DATE DATE
);

--Delivery Point Address
CREATE TABLE __schema__.abp_delivery_point (
                                               RECORD_IDENTIFIER SMALLINT,
                                               CHANGE_TYPE CHARACTER VARYING(1),
                                               PRO_ORDER BIGINT,
                                               UPRN BIGINT,
                                               UDPRN BIGINT,
                                               ORGANISATION_NAME CHARACTER VARYING(60),
                                               DEPARTMENT_NAME CHARACTER VARYING(60),
                                               SUB_BUILDING_NAME CHARACTER VARYING(30),
                                               BUILDING_NAME CHARACTER VARYING(80),
                                               BUILDING_NUMBER SMALLINT,
                                               DEPENDENT_THOROUGHFARE CHARACTER VARYING(80),
                                               THOROUGHFARE CHARACTER VARYING(80),
                                               DOUBLE_DEPENDENT_LOCALITY CHARACTER VARYING(50),
                                               DEPENDENT_LOCALITY CHARACTER VARYING(50),
                                               POST_TOWN CHARACTER VARYING(30),
                                               POSTCODE CHARACTER VARYING(8),
                                               POSTCODE_TYPE CHARACTER VARYING(1),
                                               DELIVERY_POINT_SUFFIX CHARACTER VARYING(2),
                                               WELSH_DEPENDENT_THOROUGHFARE CHARACTER VARYING(80),
                                               WELSH_THOROUGHFARE CHARACTER VARYING(80),
                                               WELSH_DOUBLE_DEPENDENT_LOCALITY CHARACTER VARYING(50),
                                               WELSH_DEPENDENT_LOCALITY CHARACTER VARYING(50),
                                               WELSH_POST_TOWN CHARACTER VARYING(30),
                                               PO_BOX_NUMBER CHARACTER VARYING(6),
                                               PROCESS_DATE DATE,
                                               START_DATE DATE,
                                               END_DATE DATE,
                                               LAST_UPDATE_DATE DATE,
                                               ENTRY_DATE DATE
);

--LPI
CREATE TABLE __schema__.abp_lpi (
                                    RECORD_IDENTIFIER SMALLINT,
                                    CHANGE_TYPE CHARACTER VARYING(1),
                                    PRO_ORDER BIGINT,
                                    UPRN BIGINT,
                                    LPI_KEY CHARACTER VARYING(14),
                                    LANGUAGE CHARACTER VARYING(3),
                                    LOGICAL_STATUS SMALLINT,
                                    START_DATE DATE,
                                    END_DATE DATE,
                                    LAST_UPDATE_DATE DATE,
                                    ENTRY_DATE DATE,
                                    SAO_START_NUMBER SMALLINT,
                                    SAO_START_SUFFIX CHARACTER VARYING(2),
                                    SAO_END_NUMBER SMALLINT,
                                    SAO_END_SUFFIX CHARACTER VARYING(2),
                                    SAO_TEXT CHARACTER VARYING(90),
                                    PAO_START_NUMBER SMALLINT,
                                    PAO_START_SUFFIX CHARACTER VARYING(2),
                                    PAO_END_NUMBER SMALLINT,
                                    PAO_END_SUFFIX CHARACTER VARYING(2),
                                    PAO_TEXT CHARACTER VARYING(90),
                                    USRN INTEGER,
                                    USRN_MATCH_INDICATOR CHARACTER VARYING(1),
                                    AREA_NAME CHARACTER VARYING(50),
                                    LEVEL CHARACTER VARYING(30),
                                    OFFICIAL_FLAG CHARACTER VARYING(1)
);

--Organisation
CREATE TABLE __schema__.abp_organisation (
                                             RECORD_IDENTIFIER SMALLINT,
                                             CHANGE_TYPE CHARACTER VARYING(1),
                                             PRO_ORDER BIGINT,
                                             UPRN BIGINT,
                                             ORG_KEY CHARACTER VARYING(14),
                                             ORGANISATION CHARACTER VARYING(100),
                                             LEGAL_NAME CHARACTER VARYING(60),
                                             START_DATE DATE,
                                             END_DATE DATE,
                                             LAST_UPDATE_DATE DATE,
                                             ENTRY_DATE DATE
);

-- --Street
-- CREATE TABLE __schema__.abp_street (
--                             RECORD_IDENTIFIER SMALLINT,
--                             CHANGE_TYPE CHARACTER VARYING(1),
--                             PRO_ORDER BIGINT,
--                             USRN INTEGER,
--                             RECORD_TYPE SMALLINT,
--                             SWA_ORG_REF_NAMING SMALLINT,
--                             STATE SMALLINT,
--                             STATE_DATE DATE,
--                             STREET_SURFACE SMALLINT,
--                             STREET_CLASSIFICATION SMALLINT,
--                             VERSION SMALLINT,
--                             STREET_START_DATE DATE,
--                             STREET_END_DATE DATE,
--                             LAST_UPDATE_DATE DATE,
--                             RECORD_ENTRY_DATE DATE,
--                             STREET_START_X DOUBLE PRECISION,
--                             STREET_START_Y DOUBLE PRECISION,
--                             STREET_END_X DOUBLE PRECISION,
--                             STREET_END_Y DOUBLE PRECISION,
--                             STREET_TOLERANCE SMALLINT
-- );

CREATE TABLE __schema__.abp_street
(
    RECORD_IDENTIFIER SMALLINT,
    CHANGE_TYPE CHARACTER VARYING(1),
    PRO_ORDER BIGINT,
    USRN INTEGER,
    RECORD_TYPE SMALLINT,
    SWA_ORG_REF_NAMING SMALLINT,
    STATE SMALLINT,
    STATE_DATE DATE,
    STREET_SURFACE SMALLINT,
    STREET_CLASSIFICATION SMALLINT,
    VERSION SMALLINT,
    STREET_START_DATE DATE,
    STREET_END_DATE DATE,
    LAST_UPDATE_DATE DATE,
    RECORD_ENTRY_DATE DATE,
    STREET_START_X DOUBLE PRECISION,
    STREET_START_Y DOUBLE PRECISION,
    STREET_START_LAT DOUBLE PRECISION,
    STREET_START_LONG DOUBLE PRECISION,
    STREET_END_X DOUBLE PRECISION,
    STREET_END_Y DOUBLE PRECISION,
    STREET_END_LAT DOUBLE PRECISION,
    STREET_END_LONG DOUBLE PRECISION,
    STREET_TOLERANCE SMALLINT
);

--Street Descriptor
CREATE TABLE __schema__.abp_street_descriptor (
                                                  RECORD_IDENTIFIER SMALLINT,
                                                  CHANGE_TYPE CHARACTER VARYING(1),
                                                  PRO_ORDER BIGINT,
                                                  USRN INTEGER,
                                                  STREET_DESCRIPTION CHARACTER VARYING(110),
                                                  LOCALITY CHARACTER VARYING(50),
                                                  TOWN_NAME CHARACTER VARYING(30),
                                                  ADMINISTRATIVE_AREA CHARACTER VARYING(30),
                                                  LANGUAGE CHARACTER VARYING(3),
                                                  START_DATE DATE,
                                                  END_DATE DATE,
                                                  LAST_UPDATE_DATE DATE,
                                                  ENTRY_DATE DATE
);

--Successor Records
CREATE TABLE __schema__.abp_successor (
                                          RECORD_IDENTIFIER SMALLINT,
                                          CHANGE_TYPE CHARACTER VARYING(1),
                                          PRO_ORDER BIGINT,
                                          UPRN BIGINT,
                                          SUCC_KEY CHARACTER VARYING(14),
                                          START_DATE DATE,
                                          END_DATE DATE,
                                          LAST_UPDATE_DATE DATE,
                                          ENTRY_DATE DATE,
                                          SUCCESSOR BIGINT
);