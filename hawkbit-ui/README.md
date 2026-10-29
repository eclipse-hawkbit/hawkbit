# hawkBit UI

Web UI for hawkBit based on [Vaadin Flow](https://vaadin.com/flow) (Spring Boot application,
`org.eclipse.hawkbit.ui.HawkbitUiApp`). The UI has no own database - it works entirely over the hawkBit Management API
(via `hawkbit-sdk-commons` Feign clients over `hawkbit-mgmt-api` interfaces), so it needs a running
hawkbit-update-server or hawkbit-mgmt-server. User guide: [hawkBit UI](../docs/ui.md).

## Build

The build needs Node.js / npm (the `vaadin-maven-plugin` runs `prepare-frontend` and `build-frontend` in the `compile`
phase and always makes a production frontend bundle - `forceProductionBuild=true`):

```
$ mvn clean install -pl hawkbit-ui -am
```

The repackaged executable jar is `hawkbit-ui/target/hawkbit-ui-0-SNAPSHOT.jar` (also installed in the maven repository).

Vaadin generates frontend files (`package.json`, `package-lock.json`, `vite.config.ts`, `tsconfig.json`, `types.d.ts`,
`node_modules`, `src/main/frontend/*`, ...) during the build. They are ignored by `hawkbit-ui/.gitignore`.

Styling uses the Lumo theme (`@StyleSheet(Lumo.STYLESHEET)` on `HawkbitUiApp`) plus the custom
`src/main/resources/META-INF/resources/styles.css`.

## Run

```
$ java -jar hawkbit-ui/target/hawkbit-ui-0-SNAPSHOT.jar --hawkbit.server.mgmtUrl=http://localhost:8080
```

Or run `org.eclipse.hawkbit.ui.HawkbitUiApp` from the IDE, or `mvn` in `hawkbit-ui` (default goal is
`spring-boot:run`).

Open http://localhost:8088 and log in with a user of the Management API (e.g. `admin`/`admin` for the default
update server setup) - the credentials are validated against `<mgmtUrl>/rest/v1/rollouts` and then used for all
Management API calls.

## Configuration

| Property                                        | Description                                                         | Default                 |
|-------------------------------------------------|---------------------------------------------------------------------|-------------------------|
| `server.port` (env `PORT`)                      | HTTP port                                                           | `8088`                  |
| `hawkbit.server.mgmtUrl`                        | hawkBit Management API base URL                                     | `http://localhost:8080` |
| `hawkbit.server.security.oauth2.client.enabled` | Enable OIDC login (with standard `spring.security.oauth2.client.*`) | `false`                 |

See [OpenID Connect](../docs/authorization.md#openid-connect) for the OIDC setup. Docker Compose files with UI
(`*-with-ui-*.yml`) are in [docker/mysql](../docker/mysql) and [docker/postgres](../docker/postgres).
