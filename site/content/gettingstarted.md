---
title: Getting Started
weight: 20
---

## On Sandbox

We offer a sandbox installation that is free for everyone to try out hawkBit's [Management UI](/hawkbit/ui/),
[Management API](/hawkbit/apis/management_api/), and [Direct Device Integration API](/hawkbit/apis/ddi_api/): &nbsp;
**[<i class="fas fa-desktop">&nbsp;</i> https://hawkbit.eclipseprojects.io](https://hawkbit.eclipseprojects.io)**

{{% warning %}}
The sandbox is a shared installation that will be reset from time to time. Therefore, it is not allowed to upload
any personal data.
{{% /warning %}}

In addition, the following vendors offer free trial accounts for their Eclipse hawkBit compatible products:

* [Bosch IoT Rollouts](https://bosch-iot-suite.com/service/rollouts/) (
  by [Bosch Digital](https://www.bosch-digital.com))
* [Kynetics Update Factory](https://www.kynetics.com/update-factory) (by [Kynetics LLC](https://www.kynetics.com/))

## From Docker Image

### Overview

HawkBit Update Server username/password -> admin/admin as default login credentials. They can be overridden by the
environment variables spring.security.user.name and spring.security.user.password which are defined in the corresponding
default [application.properties](hawkbit-runtime/hawkbit-update-server/src/main/resources/application.properties).

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
$ mvn clean install
```

### 2: Start hawkBit [update server](https://github.com/eclipse-hawkbit/hawkbit/tree/master/hawkbit-runtime/hawkbit-update-server) (Monolith)

```sh
$ java -jar ./hawkbit-runtime/hawkbit-update-server/target/hawkbit-update-server-#version#-SNAPSHOT.jar
```

### 3: Build hawkBit examples

```sh
$ git clone https://github.com/eclipse-hawkbit/hawkbit-examples.git
$ cd hawkbit-examples
$ mvn clean install
```

### 4: Start hawkBit [Device Simulator](https://github.com/eclipse-hawkbit/hawkbit-examples/tree/master/hawkbit-device-simulator)

```sh
$ java -jar ./hawkbit-device-simulator/target/hawkbit-device-simulator-#version#.jar
```

### 5: Generate Getting Started data with [Example Management API Client](https://github.com/eclipse-hawkbit/hawkbit-examples/tree/master/hawkbit-example-mgmt-simulator)

```sh
$ java -jar ./hawkbit-example-mgmt-simulator/target/hawkbit-example-mgmt-simulator-#version#.jar
```
