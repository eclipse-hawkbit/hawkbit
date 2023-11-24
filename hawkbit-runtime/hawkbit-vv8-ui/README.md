# hawkBit Vaadin 8 UI (Deprecated)
The hawkBit Vaadin v8 UI is a standalone spring-boot application with an embedded servlet container. It should be started with at least hawkbit-mgmt-server.

## On your own workstation

### Run

```bash
java -jar hawkbit-runtime/hawkbit-vv8-ui/target/hawkbit-vv8-ui-*-SNAPSHOT.jar
```

_(Note: you have to add the JDBC driver also to your class path if you intend to use another database than H2.)_

Or:

```bash
run org.eclipse.hawkbit.app.vv8ui.Vv8UIStart
```

### Usage
The Management UI can be accessed via http://localhost:8082/UI

### Clustering (Experimental!!!)
The micro-service instances are configured to communicate via Spring Cloud Bus. You could run multiple instances of any micro-service but hawkbit-mgmt-server. Management server run some schedulers which shall not run simultaneously - e.g. auto assignment checker and rollouts executor. To run multiple management server instances you shall do some extensions of hawkbit to ensure that they wont run schedulers simultaneously or you shall configure all instances but one to do not run schedulers!

## Optional Protostuff for Sprign cloud bus
The micro-service instances are configured to communicate via Spring Cloud Bus. Optionally, you could use [Protostuff](https://github.com/protostuff/protostuff) based message payload serialization for improved performance.

**Note**: If Protostuff is enabled it shall be enabled on all microservices!

Add/Uncomment to/in your `application.properties` :

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