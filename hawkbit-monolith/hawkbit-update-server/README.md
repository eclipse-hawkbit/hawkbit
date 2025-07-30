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
