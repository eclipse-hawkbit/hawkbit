# hawkBit Update Server

The hawkBit Update Server is a standalone spring-boot application with an embedded servlet container .

## On your own workstation

### Run

```bash
java -jar hawkbit-runtime/hawkbit-update-server/target/hawkbit-update-server-*-SNAPSHOT.jar
```

_(Note: you have to add the JDBC driver also to your class path if you intend to use another database than H2.)_

Or:

```bash
run org.eclipse.hawkbit.app.Start
```

### Usage

The Management UI can be accessed via http://localhost:8080/UI
The Management API can be accessed via http://localhost:8080/rest/v1

## Enable Clustering (experimental)

Clustering in hawkBit is based on _Spring Cloud Bus_. It is not enabled in the example app by default.

Add to your `application.properties` :

```properties
spring.cloud.bus.enabled=true
```

Add to your `pom.xml` :

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-stream-binder-rabbit</artifactId>
</dependency>
```

Optional as well is the addition of [Protostuff](https://github.com/protostuff/protostuff) based message payload serialization for improved performance.

Add to your `application.properties` :

```properties
spring.cloud.stream.bindings.springCloudBusInput.content-type=application/binary+protostuff
spring.cloud.stream.bindings.springCloudBusOutput.content-type=application/binary+protostuff
```

Add to your `pom.xml` :

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
