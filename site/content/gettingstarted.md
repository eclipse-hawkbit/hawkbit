---
title: Getting Started
weight: 20
---

## From Docker Image

### Overview

HawkBit Update Server default user has _admin/admin_ as default credentials. See below how the user can be changed.

It supports two configurations:

* monolith - hawkbit-update-server
* micro-service - hawkbit-mgmt-server, hawkbit-ddi-server, hawkbit-dmf-server.

### A: Run hawkBit Update Server (Monolith) as Docker Container

Start the hawkBit Update Server as a single container

```bash
$ docker run -p 8080:8080 hawkbit/hawkbit-update-server:latest
```
This will start hawkBit update server with embedded H2 database for evaluation purposes.

### B: Run hawkBit Update Server (Monolith) with services as Docker Compose
Start the hawkBit Update Server together with an PostgreSQL and RabbitMQ instance as containers

```bash
$ git clone https://github.com/eclipse-hawkbit/hawkbit.git
$ cd hawkbit/docker
$ docker-compose -f docker-compose-monolith-postgres.yml up -d
```
or with MySQL
```bash
$ git clone https://github.com/eclipse-hawkbit/hawkbit.git
$ cd hawkbit/docker
$ docker-compose -f docker-compose-monolith-mysql.yml up -d
```

If you want to start also the Simple UI, you can use, for PostgreSQL:
```bash
$ git clone https://github.com/eclipse-hawkbit/hawkbit.git
$ cd hawkbit/docker
$ docker-compose -f docker-compose-monolith-with-simple-ui-postgres.yml up -d
```
or with MySQL
```bash
$ git clone https://github.com/eclipse-hawkbit/hawkbit.git
$ cd hawkbit/docker
$ docker-compose -f docker-compose-monolith-simple-ui-mysql.yml up -d
```

Note: _-d_ flag is used to run the containers in detached mode. If you want to see the logs, you can remove the flag.

### C: Run hawkBit Update Server (Micro-Service) with services as Docker Compose
Start the hawkBit Update Server together with an PostgreSQL and RabbitMQ instance as containers

```bash
$ git clone https://github.com/eclipse-hawkbit/hawkbit.git
$ cd hawkbit/docker
$ docker-compose -f docker-compose-micro-services-postgres.yml up -d
```
or with MySQL
```bash
$ git clone https://github.com/eclipse-hawkbit/hawkbit.git
$ cd hawkbit/docker
$ docker-compose -f docker-compose-micro-services-mysql.yml up -d
```

If you want to start also the Simple UI, you can use, for PostgreSQL:
```bash
$ git clone https://github.com/eclipse-hawkbit/hawkbit.git
$ cd hawkbit/docker
$ docker-compose -f docker-compose-micro-services-with-simple-ui-postgres.yml up -d
```
or with MySQL
```bash
$ git clone https://github.com/eclipse-hawkbit/hawkbit.git
$ cd hawkbit/docker
$ docker-compose -f docker-compose-micro-services-simple-ui-mysql.yml up -d
```

Note: _-d_ flag is used to run the containers in detached mode. If you want to see the logs, you can remove the flag.

## From Sources

### 1: Clone and build hawkBit

```sh
$ git clone https://github.com/eclipse-hawkbit/hawkbit.git
$ cd hawkbit
$ mvn clean install -DskipTests
```

### 2: Start hawkBit [update server](https://github.com/eclipse-hawkbit/hawkbit/tree/master/hawkbit-runtime/hawkbit-update-server) (Monolith)

```sh
$ java -jar ./hawkbit-monolith/hawkbit-update-server/target/hawkbit-update-server-0-SNAPSHOT.jar
```

Note: you could start it also in microservices mode by:
```sh
$ java -jar ./hawkbit-mgmt/hawkbit-mgmt-server/target/hawkbit-mgmt-server-0-SNAPSHOT.jar
```
```sh
$ java -jar ./hawkbit-ddi/hawkbit-ddi-server/target/hawkbit-ddi-server-0-SNAPSHOT.jar
```
and (only if you want to use the DMF feature):

```sh
$ java -jar ./hawkbit-dmf/hawkbit-dmf-server/target/hawkbit-dmf-server-0-SNAPSHOT.jar
```

Note: you could starte the Simple UI by:
```sh
$ java -jar ./hawkbit-simple-ui/target/hawkbit-simple-ui-0-SNAPSHOT.jar
```

## Configuration
### Change credentials
As stated before the default user is _admin/admin_. It could be overridden by changing the [TenantAwareUserProperties](https://github.com/eclipse-hawkbit/hawkbit/blob/master/hawkbit-core/src/main/java/org/eclipse/hawkbit/tenancy/TenantAwareUserProperties.java) configuration using Spring ways. For instance, using a properties file like:
```properties 
# should remove the admin/admin user
hawkbit.security.user.admin.tenant=#{null}
hawkbit.security.user.admin.password=#{null}
hawkbit.security.user.admin.roles=#{null}
# should add a hawkbit/isAwesome! user
hawkbit.security.user.hawkbit.tenant=DEFAULT
hawkbit.security.user.hawkbit.password={noop}isAwesome!
hawkbit.security.user.hawkbit.roles=TENANT_ADMIN
```
which should remove the default _admin/admin_ user and add a hawkbit user _hawkbit_ with password _isAwesome!_ and a role _TENANT_ADMIN_. 

You could create multiple users with specified roles.