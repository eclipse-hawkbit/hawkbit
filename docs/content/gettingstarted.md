---
title: Getting Started
weight: 20
---

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

## From Sources

### Clone and build hawkBit
```sh
$ git clone https://github.com/eclipse/hawkbit.git
$ cd hawkbit
$ mvn clean install
```

### Start hawkBit 
[update server](https://github.com/eclipse/hawkbit/tree/master/hawkbit-runtime/hawkbit-update-server)

```sh
$ java -jar ./hawkbit-runtime/hawkbit-update-server/target/hawkbit-update-server-#version#-SNAPSHOT.jar
```

### Build hawkBit examples

```sh
$ git clone https://github.com/eclipse/hawkbit-examples.git
$ cd hawkbit-examples
$ mvn clean install
```

### Start hawkBit device simulator
[Device Simulator](https://github.com/eclipse/hawkbit-examples/tree/master/hawkbit-device-simulator)
```sh
$ java -jar ./hawkbit-device-simulator/target/hawkbit-device-simulator-#version#.jar
```

### Generate Getting Started data
[Example Management API Client](https://github.com/eclipse/hawkbit-examples/tree/master/hawkbit-example-mgmt-simulator)

```sh
$ java -jar ./hawkbit-example-mgmt-simulator/target/hawkbit-example-mgmt-simulator-#version#.jar
```