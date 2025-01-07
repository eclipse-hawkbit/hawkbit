---
title: Run hawkBit
parent: Guides
weight: 31
---

In this guide we describe how to run a full featured hawkBit setup based on a production ready infrastructure. It is
based on the hawkBit example modules and update server.

<!--more-->

{{% note %}}
The update server can in fact be run stand alone. However, only with an embedded H2, no Device Management Federation API
and no artifact storage.
{{% /note %}}

## System Architecture

This guide describes a target architecture that is more like one that you will expect in a production system.

- hawkBit [Update Server](https://github.com/eclipse-hawkbit/hawkbit/tree/master/hawkbit-monolith/hawkbit-update-server).
- [MySQL](https://www.mysql.com/) for the repository.
- [RabbitMQ](https://www.rabbitmq.com) for DMF communication.
- For testing and demonstration purposes we will also use:
- [hawkBit Device Simulator](https://github.com/eclipse-hawkbit/hawkbit-examples/tree/master/hawkbit-device-simulator).
- [hawkBit Management API example client](https://github.com/eclipse-hawkbit/hawkbit-examples/tree/master/hawkbit-example-mgmt-feign-client).

## Prerequisites

- You have a working [hawkBit core build](https://github.com/eclipse-hawkbit/hawkbit).
- You have a working [hawkBit examples build](https://github.com/eclipse-hawkbit/hawkbit-examples).

## Adapt hawkBit Update Server and Device Simulator to your environment.

As mentioned you can create your own application with hawkBit inside or adapt the existing example app. The second
option will be shown here.

### Configure MySQL connection settings.

For this you can either edit the existing _application.properties_ or create
a [new profile](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-external-config-profile-specific-properties).

```properties
spring.jpa.database=MYSQL
spring.datasource.url=jdbc:mysql://localhost:3306/YOUR_SCHEMA
spring.datasource.username=YOUR_USER
spring.datasource.password=YOUR_PWD
spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver
```

### Configure RabbitMQ connection settings for update server and device simulator (optional).

We provide already defaults that should work with a standard Rabbit installation. Otherwise configure the following in
the `application.properties` of the two services:

```properties
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
spring.rabbitmq.virtualHost=/
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
```

### Adapt hostname of example scenario [creation script](https://github.com/eclipse-hawkbit/hawkbit-examples/blob/master/hawkbit-example-mgmt-simulator/src/main/resources/application.properties)

Should only be necessary if your system does not run on localhost or uses a different port than the example app.

Adapt `application.properties` in this case:

```properties
hawkbit.url=localhost:8080
```

or provide the parameter on command line:

```properties
hawkbit-example-mgmt-simulator-##VERSION##.jar --hawkbit.url=YOUR_HOST:PORT
```

## Compile & Run

### Compile & Run your _"production ready"_ app

see [update server](https://github.com/eclipse-hawkbit/hawkbit/tree/master/hawkbit-monolith/hawkbit-update-server)

### Compile & Run example scenario [creation script](https://github.com/eclipse-hawkbit/hawkbit-examples/tree/master/hawkbit-example-mgmt-simulator) (optional)

This has to be done before the device simulator is started. hawkBit creates the mandatory tenant metadata with first
login into either Management API (which is done by this client).

However, this is not done by _DMF_ which is in fact used by the device simulator, i.e. without calling _Management API_
first hawkBit would drop all _DMF_ messages as the tenant is unknown.

### Compile & Run device simulator (optional)

see [device simulator](https://github.com/eclipse-hawkbit/hawkbit-examples/tree/master/hawkbit-device-simulator)

# Enjoy hawkBit with a real database, artifact storage and all [interfaces](../../apis/) available
