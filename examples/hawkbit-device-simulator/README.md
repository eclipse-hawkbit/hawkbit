# hawkBit Device Simulator

The device simulator handles software update commands from the update server.

## Run
```
java -jar examples/hawkbit-device-simulator/target/hawkbit-device-simulator-*-SNAPSHOT.jar
```
Or:
```
run org.eclipse.hawkbit.simulator.DeviceSimulator
```

## Usage
The device simulator exposes an REST-API which can be used to trigger device creation.
```
http://localhost:8083/start?amount=10
```
