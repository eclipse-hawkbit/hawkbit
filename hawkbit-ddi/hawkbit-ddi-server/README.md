# hawkBit DDI Server (EXPERIMENTAL!)

The hawkBit DDI Server is a standalone spring-boot application with an embedded servlet container. It should be started
with at least hawkbit-mgmt-server.

## On your own workstation

### Run

```bash
java -jar hawkbit-ddi/hawkbit-ddi-server/target/hawkbit-ddi-server-0-SNAPSHOT.jar
```

_(Note: you have to add the JDBC driver also to your class path if you intend to use another database than H2.)_

Or:

```bash
run org.eclipse.hawkbit.app.ddi.DDIStart
```

### Usage

The Management API can be accessed via http://localhost:8081/rest/v1
The root url http://localhost:8081 will redirect directly to the Swagger Management UI

# Clustering (Experimental!!!)
## Remote Events between micro-services
[See more information](../../docs/content/guides/clustering.md)