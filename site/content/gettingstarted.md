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

### B: Run hawkBit Update Server (Monolith) with services as Docker Compose

Start the hawkBit Update Server together with an MySQL and RabbitMQ instance as containers

```bash
$ git clone https://github.com/eclipse-hawkbit/hawkbit.git
$ cd hawkbit/hawkbit-runtime/docker
$ docker-compose -f docker-compose-monolith-mysql.yml up -d
```

### C: Run hawkBit Update Server (Micro-Service) with services as Docker Compose

Start the hawkBit Update Server together with an MySQL and RabbitMQ instance as containers

```bash
$ git clone https://github.com/eclipse-hawkbit/hawkbit.git
$ cd hawkbit/hawkbit-runtime/docker
$ docker-compose -f docker-compose-micro-service-mysql.yml up -d
```

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

### 3: Change credentials
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