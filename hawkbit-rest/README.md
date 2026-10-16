# Eclipse.IoT hawkBit - REST Common

Shared REST building blocks used by the Management API and the DDI API modules (`hawkbit-mgmt-*`, `hawkbit-ddi-*`).

| Module              | Description                                                                                                                                                                                                                                                            |
|---------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `hawkbit-rest-api`  | Lightweight API-level classes (no server dependencies), used by the `*-api` modules and clients (SDK): `OpenApi` (OpenAPI constants, e.g. `hawkbit.server.openapi.enabled`), `ApiResponsesConstants` (common Swagger response descriptions), shared JSON model (`ExceptionInfo`, `ResponseList`). |
| `hawkbit-rest-core` | Server-side REST infrastructure, used by the `*-resource` modules: `RestConfiguration` (HAL hypermedia, global exception handling mapping to `ExceptionInfo`), `SecurityManagedConfiguration` (DoS filter registration, HTTP firewall, `hawkbit-rest-defaults.properties`), `security.DosFilter`, `util` (file streaming, HTTP helpers, REST request logging - `@Log`, `LogAutoConfiguration` / `hawkbit.rest.log.*`), REST specific exceptions. |

# Compile

#### Build hawkbit-rest

```
$ cd hawkbit-rest
$ mvn clean install
```
