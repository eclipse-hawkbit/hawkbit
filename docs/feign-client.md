# Feign Client

In this guide we describe how to create a **Feign REST Client** for hawkBit APIs.

---

### API Modules

hawkBit provides Java API modules for the **Management API** and the **DDI API**:

- [hawkbit-mgmt-api](https://github.com/eclipse-hawkbit/hawkbit/tree/master/hawkbit-mgmt/hawkbit-mgmt-api) - see
  [Management API](management-api.md)
- [hawkbit-ddi-api](https://github.com/eclipse-hawkbit/hawkbit/tree/master/hawkbit-ddi/hawkbit-ddi-api) - see
  [Direct Device Integration API](direct-device-integration-api.md)

They contain:

- `*RestApi` interfaces (e.g. `MgmtTargetRestApi`, `MgmtRolloutRestApi`, `DdiRootControllerRestApi`) in the
  `org.eclipse.hawkbit.mgmt.rest.api` / `org.eclipse.hawkbit.ddi.rest.api` packages - the REST resources declared with
  Spring MVC annotations (`@GetMapping`, `@PostMapping`, `@PathVariable`, ...) with full request paths
- request / response DTOs in the `org.eclipse.hawkbit.mgmt.json.model` / `org.eclipse.hawkbit.ddi.json.model`
  packages (e.g. `MgmtTarget`, `MgmtTargetRequestBody`)

The servers implement these interfaces, so a client could be created from the same interfaces with
Feign's `SpringMvcContract`.

```xml
<dependency>
    <groupId>org.eclipse.hawkbit</groupId>
    <artifactId>hawkbit-mgmt-api</artifactId>
</dependency>
```

---

### Recommended: hawkBit SDK

The [hawkBit SDK](hawkbit-sdk.md) already builds Feign clients from the `*RestApi` interfaces (`HawkbitClient` in
`hawkbit-sdk-commons`). It configures the base URLs (`hawkbit.server.mgmt-url` / `hawkbit.server.ddi-url`), the
authentication (basic auth for Management API, gateway / target token for DDI API), HAL aware JSON encoding/decoding,
multipart uploads and TLS.

```java
HawkbitClient hawkbitClient = new HawkbitClient(new HawkbitServer()); // defaults: mgmt http://localhost:8080, ddi http://localhost:8081
Tenant tenant = new Tenant(); // defaults: tenant DEFAULT, admin/admin

MgmtTargetRestApi targetApi = hawkbitClient.mgmtService(MgmtTargetRestApi.class, tenant);
targetApi.createTargets(List.of(
        new MgmtTargetRequestBody().setControllerId("00-FF-AA-0").setName("00-FF-AA-0")));
MgmtTarget target = targetApi.getTarget("00-FF-AA-0").getBody();
```

For DDI use `hawkbitClient.ddiService(DdiRootControllerRestApi.class, tenant, controller)`.

---

### Custom Feign Client

If you don't want to use the SDK you could build a Feign client yourself, the same way `HawkbitClient` does - with
`SpringMvcContract`, targeting the server base URL (the paths are part of the interface mappings):

```java
MgmtTargetRestApi targetApi = Feign.builder()
        .contract(new SpringMvcContract())
        .encoder(encoder) // JSON encoder, e.g. feign-jackson
        .decoder(new ResponseEntityDecoder(decoder)) // the API methods return ResponseEntity
        .requestInterceptor(new BasicAuthRequestInterceptor("admin", "admin"))
        .target(MgmtTargetRestApi.class, "http://localhost:8080");
```

Or, with Spring Cloud OpenFeign, use the *Feign inheritance support* - declare an interface extending the API one and
autowire it:

```java
@FeignClient(name = "mgmtTargets", url = "${hawkbit.server.mgmt-url:http://localhost:8080}")
public interface MgmtTargetClient extends MgmtTargetRestApi {
}
```

> **Note:** Management API responses are HAL JSON (`application/hal+json`), so the decoder shall be able to handle HAL
> (e.g. with the Spring HATEOAS `HalJacksonModule`, as the SDK does). Multipart endpoints (artifact upload) are not well
> supported by plain Feign - the SDK handles them separately.
