# Base hawkBit setup

In this guide we describe how to setup a full featured hawkBit based on a production ready infrastructure.  
It is based on the hawkBit update server.

The update server can in fact be run stand alone. However, only with an embedded H2, no Device Management Federation API
and no artifact storage.

---

### System Architecture

This guide describes a target architecture that is more like one that you will expect in a production system:

- [hawkBit](https://github.com/eclipse-hawkbit/hawkbit/tree/master/hawkbit-monolith/hawkbit-update-server) Update Server
- [MariaDB](https://mariadb.org/) / MySQL or [PostgreSQL](https://www.postgresql.org/) for the repository
- [RabbitMQ](https://www.rabbitmq.com/) for DMF communication

---

### Prerequisites

- You have a working hawkBit [core build](https://github.com/eclipse-hawkbit/hawkbit).

As mentioned you can create your own application with hawkBit inside.

---

### Configure MariaDB/MySQL connection settings

For this you can either edit the existing `application.properties`
or [create a new profile](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-external-config-profile-specific-properties):

```properties
spring.jpa.database: MYSQL
spring.datasource.url: jdbc:mariadb://localhost:3306/YOUR_SCHEMA
spring.datasource.username: YOUR_USER
spring.datasource.password: YOUR_PWD
spring.datasource.driverClassName: org.mariadb.jdbc.Driver
```

The update server ships with a `mysql` profile (`application-mysql.properties`) with these settings for a local
MariaDB/MySQL (`jdbc:mariadb://localhost:3306/hawkbit`, user `root`), which you can activate with
`--spring.profiles.active=mysql`.

---

### Configure PostgreSQL connection settings

Alternatively, you can use PostgreSQL:

```properties
spring.jpa.database: POSTGRESQL
spring.datasource.url: jdbc:postgresql://localhost:5432/YOUR_SCHEMA
spring.datasource.username: YOUR_USER
spring.datasource.password: YOUR_PWD
spring.datasource.driverClassName: org.postgresql.Driver
```

The update server ships with a `postgresql` profile (`application-postgresql.properties`) with these settings for a local
PostgreSQL (`jdbc:postgresql://localhost:5432/hawkbit`, user `postgres`), which you can activate with
`--spring.profiles.active=postgresql`.

---

### Configure RabbitMQ (optional)

RabbitMQ is needed for the Device Management Federation API (DMF) and for the remote events between hawkBit instances
(see [Clustering](clustering.md)). In the update server (monolith) it is **disabled by default**:

```properties
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
hawkbit.dmf.enabled=false
hawkbit.events.remote.enabled=false
```

To enable it, remove the `RabbitAutoConfiguration` exclusion (e.g. `spring.autoconfigure.exclude=`) and set
`hawkbit.dmf.enabled=true` (for DMF) and/or `hawkbit.events.remote.enabled=true` (for remote events).

Defaults are already provided for a standard Rabbit installation. Otherwise configure the following in
`application.properties` of the update server (or of each service in micro-service mode):

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

