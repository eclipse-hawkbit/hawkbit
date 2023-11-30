# hawkBit Management Server
The hawkBit Management Server is a standalone spring-boot application with an embedded servlet container. It should be started with at least one (or both) of the device interface servers - hawkbit-ddi-server or/and hawkbit-dmf-server.

## On your own workstation

### Run

```bash
java -jar hawkbit-runtime/hawkbit-mgmt-server/target/hawkbit-mgmt-server-*-SNAPSHOT.jar
```

_(Note: you have to add the JDBC driver also to your class path if you intend to use another database than H2.)_

Or:

```bash
run org.eclipse.hawkbit.app.mgmt.MgmtServerStart
```

### Usage
The Management API can be accessed via http://localhost:8080/rest/v1
The root url http://localhost:8080 will redirect directly to the Swagger Management UI

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
