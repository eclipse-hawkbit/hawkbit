# hawkBit Example Application
The hawkBit example application is a standalone spring-boot application with an embedded servlet container to host the hawkBit Update Server.

We have have described several options for you to get access to the example.

## Try out the example application in our hawkBit sandbox on Bluemix
- try out Management UI https://hawkbit.eu-gb.mybluemix.net/UI (username: admin, passwd: admin)
- try out Management API https://hawkbit.eu-gb.mybluemix.net/rest/v1/targets (don't forget basic auth header; username: admin, passwd: admin)
- try out DDI API https://hawkbit.eu-gb.mybluemix.net/DEFAULT/controller/v1/MYTESTDEVICE (authentication disabled)

## On your own workstation
### Run
```
java -jar examples/hawkbit-example-app/target/hawkbit-example-app-*-SNAPSHOT.jar
```

_(Note: you have to add the JDBC driver also to your class path if you intend to use another database than H2.)_

Or:

```
run org eclipse.hawkbit.app.Start
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
