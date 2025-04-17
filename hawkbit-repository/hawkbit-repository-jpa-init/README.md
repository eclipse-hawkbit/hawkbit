# hawkBit JPA Initializer & Migrator

A standalone tool for validating and migrating the database to the current hawkBit schema. It is used to validate, initialize or migrate the database to the current hawkBit schema. 

## Configuration
Could be configured with _hawkbit.db.\<key\>_ or <spring.database.\<key\>_ environment or system properties, with keys:
* **mode** - migrate or validate (default)
* **url** - database url
* **username** - database user - shall have the necessary permissions
* **password** - database user's password
* **sql-migration-suffixes** - flyway 'sqlMigrationSuffixes' if not the default ones (<upper case database (mariadb -> mysql)>.sql)

Where:
1. Environment properties takes precedence over the system properties
2. The _hawkbit.db.\<key\>_ properties take precedence over the _spring.database.\<key\>_ properties

There are two modes:
* **migrate** - migrate the database, only when started with parameter with key mode (environment or system property)
* **validate** - validate the database, default, only validates db and throws org.flywaydb.core.api.exception.FlywayValidateException if not in sync

**Note**: could also be configured using default flyway env properties

## Usage
The module builds executable jar with all dependencies - _hawkbit-repository-jpa-init-\<revision\>.jar_. It could be configured with environment properties and run as an executable jar:
```shell
# sets the mode - default if validate
export hawkbit_db_mode=migrate 
# sets the database url - default is local h2
export hawkbit_db_url=jdbc:mariadb://localhost:3306/hawkbit
# sets the database user - default is h2 default root - sa
export hawkbit_db_username=root
# sets the database user's password - default is empty
#export hawkbit_db_password=

# run executable jar
java -jar target/hawkbit-repository-jpa-init-0-SNAPSHOT.jar
```

It could also be configured using system properties and run as a java main class:
```shell
java -classpath target/hawkbit-repository-jpa-init-0-SNAPSHOT.jar \
  -Dhawkbit.db.mode=migrate \
  -Dhawkbit.db.url=jdbc:mariadb://localhost:3306/hawkbit \
  -Dhawkbit.db.username=root \
  -Dhawkbit.db.password= \
  org.eclipse.hawkbit.repository.jpa.init.HawkbitFlywayDbInit
```

## Purpose and usecases
If you want to do db management separately from the running services - i.e. not in mgmt-server or monolith (for instance) servers but only occasionally and on real updates, or you like to validate a database according to a hawkbit db schema version you could:
1. set spring.flyway.enabled=false in the hawkbit services (or do not pack the hawkbit-repository-jpa-flyway module into the hawkbit services)
2. use the tool to do the db management (e.g. in some pipelines)