-- Table and fields in upper case as Spring queries it that way
CREATE TABLE SP_LOCK  (
	LOCK_KEY CHAR(36) NOT NULL,
	REGION VARCHAR(100) NOT NULL,
	CLIENT_ID CHAR(36),
	CREATED_DATE TIMESTAMP NOT NULL,
	constraint SP_LOCK_PK primary key (LOCK_KEY, REGION)
);