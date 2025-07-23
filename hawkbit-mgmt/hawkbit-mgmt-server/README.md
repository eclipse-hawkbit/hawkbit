# hawkBit Management Server (EXPERIMENTAL!)

The hawkBit Management Server is a standalone spring-boot application with an embedded servlet container. It should be
started with at least one (or both) of the device interface servers - hawkbit-ddi-server or/and hawkbit-dmf-server.

## On your own workstation

### Run

```bash
java -jar hawkbit-mgmt/hawkbit-mgmt-server/target/hawkbit-mgmt-server-0-SNAPSHOT.jar
```

_(Note: you have to add the JDBC driver also to your class path if you intend to use another database than H2.)_

Or:

```bash
run org.eclipse.hawkbit.app.mgmt.MgmtServerStart
```

### Usage

The Management API can be accessed via http://localhost:8080/rest/v1
The root url http://localhost:8080 will redirect directly to the Swagger Management UI

# Clustering (Experimental!!!)
## Events

Event communication between nodes is based on  [Spring Cloud Stream](http://docs.spring.io/spring-cloud-stream/docs/current/reference/htmlsingle/).
There are different [binder implementations](http://docs.spring.io/spring-cloud-stream/docs/current/reference/htmlsingle/#_binders)
available. The _hawkbit Update Server_ uses RabbitMQ binder.

You can run multiple instances of any micro-service, including those consuming events.
However, the `hawkbit-mgmt-server` should typically be run as a single instance, as it schedules time-sensitive jobs such as auto-assignment checking and rollout execution.
If multiple management server instances are needed, you must extend hawkBit to ensure that scheduled tasks do not run concurrently.
Alternatively, configure all but one instance to disable scheduler execution.

## Event Channel Types in Spring Cloud Stream

Remote events in hawkBit are distributed through **two distinct types of channels**:

### 1. Fanout Event Channel

- Every service instance listening to `fanoutEventChannel` receives **a copy of every message**, regardless of instance count.
- Common for events that should be processed by each consumer independently
  - In-memory cache updates
  - Internal state propagation
  - Logging or auditing
- Not recommended for scenarios where only one consumer should process an event (see `serviceEventChannel` for that).

**Note**: Every instance bound to this channel will get its own copy of the message.

### 2. Service Event Channel

The `serviceEventChannel` is used to **ensure exclusive consumption of events** across service instances.
Only **one instance per consumer group** receives and processes each message, which is critical for non-idempotent or resource-sensitive operations.

- Only one instance in a consumer group receives each message.
- Ideal for external integrations, third-party API calls, or any task that must not be duplicated.
- Load-balanced across instances within the same group.


### Optional Protostuff for Spring cloud stream

The micro-service instances are configured to communicate via Spring Cloud Stream. Optionally, you could
use [Protostuff](https://github.com/protostuff/protostuff) based message payload serialization for improved performance.

**Note**: If Protostuff is enabled it shall be enabled on all microservices!

Add/Uncomment to/in your `application.properties` :

```properties
spring.cloud.stream.default.content-type=application/binary+protostuff
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
