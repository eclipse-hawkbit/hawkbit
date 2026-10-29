# hawkBit SDK

Client-side Java SDK for hawkBit. It allows you to simulate DDI / DMF devices (or gateways) and to call the Management
API from Java code. User guide: [hawkBit SDK](../docs/hawkbit-sdk.md).

## Modules

| Module                | Description                                                                                                                                                                     |
|-----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `hawkbit-sdk-commons` | Common configuration (`HawkbitServer` - `hawkbit.server.*`, `Tenant` - `hawkbit.tenant.*`) and `HawkbitClient` - creates OpenFeign clients from the hawkBit `*-api` interfaces. |
| `hawkbit-sdk-mgmt`    | Management API client support (over `hawkbit-mgmt-api`), e.g. `AuthenticationSetupHelper`.                                                                                      |
| `hawkbit-sdk-device`  | DDI device simulation (over `hawkbit-ddi-api`) - `DdiTenant`, `DdiController`, `UpdateHandler`.                                                                                 |
| `hawkbit-sdk-dmf`     | DMF device simulation over AMQP (over `hawkbit-dmf-api`, spring-amqp) - `DmfTenant`, `DmfController`, `UpdateHandler`.                                                          |
| `hawkbit-sdk-demo`    | Spring Shell demo applications using the modules above.                                                                                                                         |

The REST clients are not generated - `HawkbitClient` builds Feign proxies directly from the Spring MVC annotated API
interfaces (e.g. `MgmtTargetRestApi`, `DdiRootControllerRestApi`), so the SDK always matches the server API of the same
version.

## Build

```
$ mvn clean install -pl hawkbit-sdk/hawkbit-sdk-demo -am
```

## Demo applications

`hawkbit-sdk-demo` contains three interactive (Spring Shell, no web server) applications:

| Main class                                                | Description                                         | Shell commands                                                                                                  |
|-----------------------------------------------------------|-----------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| `org.eclipse.hawkbit.sdk.demo.device.DeviceApp`           | Single DDI device                                   | `setup`, `start`, `stop`                                                                                        |
| `org.eclipse.hawkbit.sdk.demo.multidevice.MultiDeviceApp` | Many DDI devices                                    | `setup`, `start-one --id`, `stop-one --id`, `start --prefix --offset --count`, `stop --prefix --offset --count` |
| `org.eclipse.hawkbit.sdk.demo.dmf.DmfApp`                 | Many DMF devices (requires RabbitMQ and DMF server) | `start-one --id`, `stop-one --id`, `start --prefix --offset --count`, `stop --prefix --offset --count`          |

Device ids for `start` / `stop` are `<prefix><offset + i>` (index formatted with 3 digits, e.g. `dev000`), `--offset`
defaults to `0`.

The repackaged jar starts `DeviceApp` (the `mainClass` of the `spring-boot-maven-plugin`):

```
$ java -jar hawkbit-sdk/hawkbit-sdk-demo/target/hawkbit-sdk-demo-0-SNAPSHOT.jar \
    --hawkbit.server.mgmt-url=http://localhost:8080 --hawkbit.server.ddi-url=http://localhost:8081 \
    --demo.controller.id=demo
```

`MultiDeviceApp` and `DmfApp` could be started from the IDE (run their `main` methods).

Main configuration properties (defaults in `HawkbitServer` and `Tenant`):

```properties
hawkbit.server.mgmt-url=http://localhost:8080
hawkbit.server.ddi-url=http://localhost:8081
hawkbit.tenant.tenant-id=DEFAULT
hawkbit.tenant.username=admin
hawkbit.tenant.password=admin
#hawkbit.tenant.gateway-token=
# DeviceApp only
demo.controller.id=demo
#demo.controller.securityToken=
```
