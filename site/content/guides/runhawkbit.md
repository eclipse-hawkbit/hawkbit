---
title: Run hawkBit
parent: Guides
weight: 31
---

In this guide we describe how to run a full-featured hawkBit setup based on a production ready infrastructure. It is
based on the hawkBit example modules and update server.

<!--more-->

{{% note %}}
The update server can in fact be run stand alone. However, only with an embedded H2, no Device Management Federation API
and no artifact storage.
{{% /note %}}

## System Architecture

This guide describes a target architecture that you will probably expect in a production system.

- hawkBit [Update Server](https://github.com/eclipse-hawkbit/hawkbit/tree/master/hawkbit-monolith/hawkbit-update-server).
- [MariaDB](https://mariadb.org) for the repository.
- [RabbitMQ](https://www.rabbitmq.com) for DMF communication (optional for monolith / single host deployment).

For testing, demonstration or integrations purposes you could also use hawkBit SDK:
- [hawkBit SDK Management API client](https://github.com/eclipse-hawkbit/hawkbit/blob/master/hawkbit-sdk/hawkbit-sdk-commons/src/main/java/org/eclipse/hawkbit/sdk/HawkbitClient.java).
- [hawkBit SDK / Simulator Device](https://github.com/eclipse-hawkbit/hawkbit/tree/master/hawkbit-sdk/hawkbit-sdk-device).
- [hawkBit SDK / Simulator for DMF integration](https://github.com/eclipse-hawkbit/hawkbit/tree/master/hawkbit-sdk/hawkbit-sdk-dmf)
- [hawkBit SDK Demo Devices](https://github.com/eclipse-hawkbit/hawkbit/tree/master/hawkbit-sdk/hawkbit-sdk-demo)

## Prerequisites

- You have a working [hawkBit core build](https://github.com/eclipse-hawkbit/hawkbit).

## Adapt hawkBit Update Server and Device Simulator to your environment.

As mentioned you can create your own application with hawkBit inside or adapt the existing example app. The second
option will be shown here.

### Configure MariaDB/MySQL connection settings.

For this you can either edit the existing _application.properties_ or create
a [new profile](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-external-config-profile-specific-properties).

```properties
spring.jpa.database=MYSQL
spring.datasource.driverClassName=org.mariadb.jdbc.Driver
spring.datasource.url=jdbc:mariadb://localhost:3306/<YOUR_SCHEMA>
spring.datasource.username=<YOUR_USER>
spring.datasource.password=<YOUR_PWD>
```

### Configure RabbitMQ connection settings for services (optional for monolith single host deployments).

We provide already defaults that should work with a standard Rabbit installation (spring boot RabbitProperties defaults). 
Otherwise, configure the following in the `application.properties` of the services:

```properties
spring.rabbitmq.host=<Rabbit MQ host>
spring.rabbitmq.port=<Rabbit MQ port>
spring.rabbitmq.virtualHost=<virtual host to be used>
spring.rabbitmq.username=<YOUR_USER>
spring.rabbitmq.password=<YOUR_PWD>
```

### Adapt hostnames of demo simulator

Should only be necessary if your system does not run on localhost or uses a different port than the example app.

Adapt `application.properties` or pass system / env variables for Spring properties:

```properties
hawkbit.server.mgmtUrl=<MGMT server host:MGMT port>
hawkbit.server.ddiUrl=<DDI server host:DDI port>
```

## Compile & Run

### Compile & Run your _"production ready"_ app

see [update server](https://github.com/eclipse-hawkbit/hawkbit/tree/master/hawkbit-monolith/hawkbit-update-server)

Note: you have to log into update server (e.g. via UI) before starting device / device simulator. Then hawkBit creates the mandatory tenant metadata.

### Compile & Run demo simulator (optional)

see [demo simulator](https://github.com/eclipse-hawkbit/hawkbit/tree/master/hawkbit-sdk/hawkbit-sdk-demo)

login into either Management API (which is done by this client).

## Enjoy hawkBit with a real database, artifact storage and all [interfaces](../../apis/) available
