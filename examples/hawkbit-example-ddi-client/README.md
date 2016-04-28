# Eclipse.IoT hawkBit - Example DDI Client 

The hawkBit Example DDI Client is an implementation of a simulated device/controller that communicates with the hawkBit server via the DDI API.  

# Compile, Run and Getting Started

#### Build hawkbit-example-ddi-client

```
$ cd hawkbit/examples/hawkbit-example-ddi-client
$ mvn clean install
```

#### Run hawkBit Example DDI Client

Start the [hawkBit example app](../hawkbit-example-app).

Create an instance of the hawkBit Example DDI Client and run it as thread e.g. in the ExecutorService.

```
DdiExampleClient ddiExampleClient = new DdiExampleClient("http://localhost:8080/", "controllerIdName", "DEFAULT", new DoNotSaveArtifacts());
ExecutorService executorService = Executors.newFixedThreadPool(1);
executorService.execute(ddiExampleClient);
```

#### Getting started with hawkBit Example DDI Client

After the hawkBit Example DDI Client has started it will poll once against the given hawkBit server and form then on depending on your polling configuration.
After the hawkBit Example DDI Client has polled successful your will see a target in the deployment view regarding the given controller id of your hawkBit Example DDI Client. 

