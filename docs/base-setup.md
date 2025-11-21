# Base hawkBit setup

In this guide we describe how to setup a full featured hawkBit based on a production ready infrastructure.  
It is based on the hawkBit update server.

The update server can in fact be run stand alone. However, only with an embedded H2, no Device Management Federation API and no artifact storage.

---

### System Architecture

This guide describes a target architecture that is more like one that you will expect in a production system:

- [hawkBit](https://github.com/eclipse-hawkbit/hawkbit/tree/master/hawkbit-monolith/hawkbit-update-server) Update Server
- [MariaDB](https://mariadb.org/) for the repository  
- [RabbitMQ](https://www.rabbitmq.com/) for DMF communication

---

### Prerequisites

- You have a working hawkBit [core build](https://github.com/eclipse-hawkbit/hawkbit).  

As mentioned you can create your own application with hawkBit inside.

---

### Configure MariaDB/MySQL connection settings

For this you can either edit the existing `application.properties` or [create a new profile](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-external-config-profile-specific-properties):

```properties
spring.jpa.database: MYSQL
spring.datasource.url: jdbc:mariadb://localhost:3306/YOUR_SCHEMA
spring.datasource.username: YOUR_USER
spring.datasource.password: YOUR_PWD
spring.datasource.driverClassName: org.mariadb.jdbc.Driver
```

> **Note:** On Ubuntu 18.04 with MariaDB 10.1 installed from the default repository via `apt install`,  
> the `COLLATE` option of database has to be changed manually to `latin1`.  
> For recent versions of MariaDB running on Ubuntu this is not required. (cf. [issue](https://github.com/eclipse-hawkbit/hawkbit/issues/963))

---

### Configure RabbitMQ (optional)

Defaults are already provided for a standard Rabbit installation. Otherwise configure the following in `application.properties` of the two services:

```properties
spring.rabbitmq.username: guest
spring.rabbitmq.password: guest
spring.rabbitmq.virtualHost: /
spring.rabbitmq.host: localhost
spring.rabbitmq.port: 5672
```

---

### Compile & Run

#### Compile & Run your app
See [update server](https://github.com/eclipse-hawkbit/hawkbit/tree/master/hawkbit-monolith/hawkbit-update-server)

---

Enjoy hawkBit with a real database, artifact storage and all interfaces available.

