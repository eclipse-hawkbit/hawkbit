# hawkBit Update Server
The hawkBit Update Server is a standalone spring-boot application with an embedded servlet container .

## Try out the update server in our hawkBit sandbox on Bluemix
- try out Management UI https://hawkbit.eu-gb.mybluemix.net/UI (username: admin, passwd: admin)
- try out Management API https://hawkbit.eu-gb.mybluemix.net/rest/v1/targets (don't forget basic auth header; username: admin, passwd: admin)
- try out DDI API https://hawkbit.eu-gb.mybluemix.net/DEFAULT/controller/v1/MYTESTDEVICE (authentication disabled)

## On your own workstation
### Run
```
java -jar hawkbit-runtime/hawkbit-update-server/target/hawkbit-update-server-*-SNAPSHOT.jar
```

_(Note: you have to add the JDBC driver also to your class path if you intend to use another database than H2.)_

Or:

```
run org.eclipse.hawkbit.app.Start
```

### Usage
The Management UI can be accessed via http://localhost:8080/UI
The Management API can be accessed via http://localhost:8080/rest/v1

## Deploy example app to Cloud Foundry

- Go to ```target``` subfolder.
- Select one of the two manifests
 - **manifest-simple.yml** for a standalone hawkBit installation with embedded H2.
 - **manifest.yml**  for a standalone hawkBit installation with embedded H2 and RabbitMQ service binding for DMF integration (note: this manifest is used for the sandbox above).
- Run ```cf push``` against you cloud foundry environment.

# Enable Clustering (experimental)

Clustering in hawkBit is based on _Spring Cloud Bus_. It is not enabled in the example app by default.

Add to your `application.properties` :

```
spring.cloud.bus.enabled=true
```

Add to your `pom.xml` :

```
<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-stream-binder-rabbit</artifactId>
</dependency>
```

Optional as well is the addition of [Protostuff](https://github.com/protostuff/protostuff) based message payload serialization for improved performance.


Add to your `application.properties` :

```
spring.cloud.stream.bindings.springCloudBusInput.content-type=application/binary+protostuff
spring.cloud.stream.bindings.springCloudBusOutput.content-type=application/binary+protostuff
```

Add to your `pom.xml` :

```
<dependency>
	<groupId>io.protostuff</groupId>
	<artifactId>protostuff-core</artifactId>
</dependency>
<dependency>
	<groupId>io.protostuff</groupId>
	<artifactId>protostuff-runtime</artifactId>
</dependency>
```
