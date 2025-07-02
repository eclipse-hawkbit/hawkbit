# hawkBit Update Server

The hawkBit Update Server (Monolith) is a standalone spring-boot application with an embedded servlet container .

## On your own workstation

### Run

```bash
java -jar hawkbit-monolith/hawkbit-update-server/target/hawkbit-update-server-*-SNAPSHOT.jar
```

_(Note: you have to add the JDBC driver also to your class path if you intend to use another database than H2.)_

Or:

```bash
run org.eclipse.hawkbit.doc.Start
```

### Usage

The Management API can be accessed via http://localhost:8080/rest/v1

## Enable Clustering (experimental)

Clustering in hawkBit is based on _Spring Cloud Bus_. It is enabled by default in microservice apps and disabled (by default) in the 
monolith app. To enable it for monolith app you should set (via environment, system properties or properties files) the following:

Add to your `pom.xml` :

```properties
spring.autoconfigure.exclude=
spring.cloud.bus.enabled=true
```

Optional as well is the addition of [Protostuff](https://github.com/protostuff/protostuff) based message payload
serialization for improved performance. To enable it set (via environment, system properties or properties files):

```properties
spring.cloud.stream.bindings.springCloudBusInput.content-type=application/binary+protostuff
spring.cloud.stream.bindings.springCloudBusOutput.content-type=application/binary+protostuff
```

and add to your `pom.xml` :

```xml
<dependency>
  <groupId>io.protostuff</groupId>
  <artifactId>protostuff-core</artifactId>
</dependency>
<dependency>
  <groupId>io.protostuff</groupId>
  <artifactId>protostuff-runtime</artifactId>
</dependency>
```
