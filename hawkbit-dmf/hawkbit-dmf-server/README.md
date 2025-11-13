# hawkBit DMF Server
The hawkBit DMF Server is a standalone spring-boot application with an embedded servlet container. It should be started
with at least hawkbit-mgmt-server.

## On your own workstation

### Run
```bash
java -jar hawkbit-dmf/hawkbit-dmf-server/target/hawkbit-dmf-server-0-SNAPSHOT.jar
```
_(Note: you have to add the JDBC driver also to your class path if you intend to use another database than H2.)_

Or:
```bash
run org.eclipse.hawkbit.app.dmf.DMFStart
```
# Clustering (Experimental!!!)
## Remote Events between micro-services
[See more information](../../docs/content/guides/clustering.md)