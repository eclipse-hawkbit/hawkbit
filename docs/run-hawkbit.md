# Run hawkBit

In this guide we describe how to run a full featured hawkBit setup based on a production ready infrastructure.  
It is based on the hawkBit example modules and update server.

The update server can in fact be run stand alone. However, only with an embedded H2, no Device Management Federation API and no artifact storage.

---

### System Architecture

This guide describes a target architecture that is more like one that you will expect in a production system:

- [hawkBit](https://github.com/eclipse-hawkbit/hawkbit/tree/master/hawkbit-monolith/hawkbit-update-server) Update Server
- [MariaDB](https://mariadb.org/) for the repository  
- [RabbitMQ](https://www.rabbitmq.com/) for DMF communication  

For testing and demonstration purposes we will also use:

- hawkBit [Device Simulator](https://github.com/eclipse-hawkbit/hawkbit-examples/tree/master/hawkbit-device-simulator)
- hawkBit [Management API example client](https://github.com/eclipse-hawkbit/hawkbit-examples/tree/master/hawkbit-example-mgmt-feign-client)

---

### Prerequisites

- You have a working hawkBit [core build](https://github.com/eclipse-hawkbit/hawkbit).  
- You have a working hawkBit [examples build](https://github.com/eclipse-hawkbit/hawkbit-examples).  
- Adapt hawkBit Update Server and Device Simulator to your environment.  

As mentioned you can create your own application with hawkBit inside or adapt the existing example app.  
The second option will be shown here.

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

For update server and device simulator.  
Defaults are already provided for a standard Rabbit installation. Otherwise configure the following in `application.properties` of the two services:

```properties
spring.rabbitmq.username: guest
spring.rabbitmq.password: guest
spring.rabbitmq.virtualHost: /
spring.rabbitmq.host: localhost
spring.rabbitmq.port: 5672
```

---

### Adapt hostname of example scenario &nbsp; [creation script](https://github.com/eclipse-hawkbit/hawkbit-examples/blob/master/hawkbit-example-mgmt-simulator/src/main/resources/application.properties)

Should only be necessary if your system does not run on `localhost` or uses a different port than the example app.  

Adapt `application.properties` in this case:

```properties
hawkbit.url: localhost:8080
```

or provide the parameter on command line:

```properties
hawkbit-example-mgmt-simulator-##VERSION##.jar --hawkbit.url=YOUR_HOST:PORT
```

---

### Compile & Run

#### Compile & Run your “production ready” app
See [update server](https://github.com/eclipse-hawkbit/hawkbit/tree/master/hawkbit-monolith/hawkbit-update-server)

#### Compile & Run example scenario creation script (optional)
This has to be done before the device simulator is started. hawkBit creates the mandatory tenant metadata with first login into either Management API (which is done by this client).  

However, this is not done by DMF which is in fact used by the device simulator, i.e. without calling Management API first hawkBit would drop all DMF messages as the tenant is unknown.

#### Compile & Run device simulator (optional)
See [device simulator](https://github.com/eclipse-hawkbit/hawkbit-examples/tree/master/hawkbit-device-simulator)

---

Enjoy hawkBit with a real database, artifact storage and all interfaces available.

