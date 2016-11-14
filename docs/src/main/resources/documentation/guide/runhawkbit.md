---
layout: documentation
title: Run hawkBit
---

{% include base.html %}

# Run hawkBit

In this guide we describe how to run a full featured hawkBit setup based on a production ready infrastructure. It is based on the _hawkBit_ example modules. We call these _examples_ as we expect that developers who intend to create a _hawkBit_ based IoT application on their own will create a custom [Spring Boot](http://projects.spring.io/spring-boot/) app based on _hawkBit_ as demonstrated with the [hawkBit example app](https://github.com/eclipse/hawkbit/tree/master/examples/hawkbit-example-app).

Note: the example app can in fact be run [stand alone](https://github.com/eclipse/hawkbit/tree/master/examples/hawkbit-example-app). However, only with an embedded H2, no [Device Management Federation API](https://github.com/eclipse/hawkbit/wiki/Device-Management-Federation-API) and no artifact storage.

This guide will focus on a complete setup that includes all _hawkBit_ features.

# System Architecture
This guide describes a target architecture that is more like one that you will expect in a production system.

- hawkBit Update Server [example app](https://github.com/eclipse/hawkbit/tree/master/examples/hawkbit-example-app).
- [MariaDB](https://mariadb.org) for the repository.
- [MongoDB](https://www.mongodb.org) for artifact storage.
- [RabbitMQ](https://www.rabbitmq.com) for DMF communication.
- For testing and demonstration purposes we will also use:
 - [hawkBit Device Simulator](https://github.com/eclipse/hawkbit/tree/master/examples/hawkbit-device-simulator).
 - [hawkBit Management API example client](https://github.com/eclipse/hawkbit/tree/master/examples/hawkbit-mgmt-api-client).

# Prerequisites

- You have a MongoDB (>= 3.0), RabbitMQ and MariaDB/MySQL installed and running in your environment.
- You have a working [hawkBit build](https://github.com/eclipse/hawkbit).

# Steps

## Adapt hawkBit Update Server and Device Simulator to your environment.

As mentioned you can create your own application with _hawkBit_ inside or adapt the existing example app. The second option will be shown here.

### Set MariaDB dependency to compile in the [example App POM](https://github.com/eclipse/hawkbit/blob/master/examples/hawkbit-example-app/pom.xml)
{% highlight plaintext %}
<dependency>
  <groupId>org.mariadb.jdbc</groupId>
  <artifactId>mariadb-java-client</artifactId>
  <scope>compile</scope>
</dependency>
{% endhighlight %}

### Configure MariaDB/MySQL and MongoDB connection settings.

For this you can either edit the existing *application.properties* or create a [new profile](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-external-config-profile-specific-properties).

{% highlight plaintext %}
spring.jpa.database=MYSQL
spring.datasource.url=jdbc:mysql://localhost:3306/YOUR_SCHEMA
spring.datasource.username=YOUR_USER
spring.datasource.password=YOUR_PWD
spring.datasource.driverClassName=org.mariadb.jdbc.Driver
spring.data.mongodb.uri=mongodb://localhost/hawkbitArtifactRepository
{% endhighlight %}

### Configure RabbitMQ connection settings for update server and device simulator (optional).

We provide already defaults that should work with a standard Rabbit installation. Otherwise configure the following in the `application.properties` of the two services:

{% highlight plaintext %}
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
spring.rabbitmq.virtualHost=/
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
{% endhighlight %}

### Adapt hostname of example scenario [creation script](https://github.com/eclipse/hawkbit/tree/master/examples/hawkbit-mgmt-api-client) (optional)

Should only be necessary if your system does not run on localhost or uses a different port than the example app.

Adapt `application.properties` in this case:
{% highlight plaintext %}
hawkbit.url=localhost:8080
{% endhighlight %}

or provide the parameter on command line:
{% highlight plaintext %}
hawkbit-example-mgmt-simulator-##VERSION##.jar --hawkbit.url=YOUR_HOST:PORT
{% endhighlight %}

## Compile & Run

### Compile & Run your _"production ready"_ app.

see [example app](https://github.com/eclipse/hawkbit/tree/master/examples/hawkbit-example-app)

### Compile & Run example scenario [creation script](https://github.com/eclipse/hawkbit/tree/master/examples/hawkbit-mgmt-api-client) (optional).

This has to be done before the device simulator is started. _hawkBit_ creates the mandatory tenant metadata with first login into either _Management UI_ or API (which is done by this client).

However, this is not done by _DMF_ which is in fact used by the device simulator, i.e. without calling _Management API_ first _hawkBit_ would drop all _DMF_ messages as the tenant is unknown.

### Compile & Run device simulator (optional).

see [device simulator](https://github.com/eclipse/hawkbit/tree/master/examples/hawkbit-device-simulator)

## Enjoy hawkBit with a real database, artifact storage and all [interfaces](https://github.com/eclipse/hawkbit/wiki/Interfaces) available.

![](../images/gettingStartedResult.png){:width="100%"}
