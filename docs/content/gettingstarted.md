---
title: Getting Started
weight: 20
---

## On Sandbox

We offer a sandbox installation that is free for everyone to try out hawkBit's [Management UI](/hawkbit/ui/), 
[Management API](/hawkbit/apis/management_api/), and [Direct Device Integration API](/hawkbit/apis/ddi_api/): &nbsp;
**[<i class="fas fa-desktop">&nbsp;</i> https://hawkbit-demo-sandbox.eu-gb.mybluemix.net/UI/](https://hawkbit-demo-sandbox.eu-gb.mybluemix.net/UI/)**

{{% warning %}}
The sandbox database will be reset from time to time. It is also not possible to upload any artifacts into the sandbox. Moreover, you are not permitted to store any kind of personal data in the sandbox.
{{% /warning %}}

In addition, the following vendors offer free trial accounts for their Eclipse hawkBit compatible products:

* [Bosch IoT Rollouts](https://www.bosch-iot-suite.com/rollouts/#plans) (by [Bosch Software Innovations GmbH](https://www.bosch-si.com/corporate/home/homepage.html))


## From Docker Image

### Update server only

```sh
$ docker run -d -p 8080:8080 hawkbit/hawkbit-update-server
```

### Updates server + MySql + RabbitMq

```sh
$ git clone https://github.com/eclipse/hawkbit.git
$ cd hawkbit/hawkbit-runtime/hawkbit-update-server/docker
$ docker-compose up -d
```
{{% note %}}
Requires Docker-Compose installed.
{{% /note %}}

## From Sources

### 1: Clone and build hawkBit
```sh
$ git clone https://github.com/eclipse/hawkbit.git
$ cd hawkbit
$ mvn clean install
```

### 2: Start hawkBit [update server](https://github.com/eclipse/hawkbit/tree/master/hawkbit-runtime/hawkbit-update-server)

```sh
$ java -jar ./hawkbit-runtime/hawkbit-update-server/target/hawkbit-update-server-#version#-SNAPSHOT.jar
```

### 3: Build hawkBit examples

```sh
$ git clone https://github.com/eclipse/hawkbit-examples.git
$ cd hawkbit-examples
$ mvn clean install
```

### 4: Start hawkBit [Device Simulator](https://github.com/eclipse/hawkbit-examples/tree/master/hawkbit-device-simulator)
```sh
$ java -jar ./hawkbit-device-simulator/target/hawkbit-device-simulator-#version#.jar
```

### 5: Generate Getting Started data with [Example Management API Client](https://github.com/eclipse/hawkbit-examples/tree/master/hawkbit-example-mgmt-simulator)

```sh
$ java -jar ./hawkbit-example-mgmt-simulator/target/hawkbit-example-mgmt-simulator-#version#.jar
```