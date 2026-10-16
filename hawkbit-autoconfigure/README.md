# hawkBit autoconfigure

Spring Boot auto-configurations for the common hawkBit server parts. Used by the starters (`hawkbit-mgmt-starter`,
`hawkbit-ddi-starter`, `hawkbit-dmf-starter`). The auto-configurations are registered in
`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

## Auto-configurations (`org.eclipse.hawkbit.autoconfigure.*`)

| Package       | Class                                                | Purpose                                                                                                         |
|---------------|------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| `repository`  | `JpaRepositoryAutoConfiguration`                     | imports `JpaRepositoryConfiguration` (if on the classpath) - the JPA repository                                 |
| `repository.event` | `EventPublisherAutoConfiguration`               | imports `EventPublisherConfiguration` - repository event publishing                                             |
| `scheduling`  | `ExecutorAutoConfiguration`                          | central `asyncExecutor`, `taskExecutor`, `scheduledExecutorService`, `taskScheduler` (security context aware), configured via `hawkbit.executor.*` (`ExecutorProperties`) |
| `scheduling`  | `AsyncConfigurerAutoConfiguration`                   | `@EnableAsync` using the `asyncExecutor` (if no other `AsyncConfigurer`)                                        |
| `security`    | `SecurityAutoConfiguration`                          | security properties (`HawkbitSecurityProperties`, ...), auditor aware, audit logging aspect, login / logout handlers |
| `security`    | `StaticUserManagementAutoConfiguration`              | in-memory (property based) user management                                                                     |
| `artifact`    | `PropertyBasedArtifactUrlResolverAutoConfiguration`  | property based artifact download URL resolver, defaults in `hawkbit-artifactdl-defaults.properties`             |
| `throttle`    | `ThrottleAutoConfiguration`                          | per-tenant DB connection throttle - only with `hawkbit.throttle.enabled=true`, see [throttling](../docs/throttling.md) |

Most beans are `@ConditionalOnMissingBean`, so they could be replaced by application specific beans.

`hawkbit.executor.*` defaults: `core-threads=5`, `max-threads=20`, `queue-size=5000`, `idle-timeout=10000` (ms),
`scheduler-threads=3`.

## Module specific auto-configurations

Some modules ship their own auto-configuration in the `org.eclipse.hawkbit.autoconfigure.*` package, registered in
their own `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, e.g.:

- `hawkbit-mgmt-starter` - `autoconfigure.mgmt.MgmtApiAutoConfiguration`, `MgmtSecurityConfiguration`
- `hawkbit-ddi-starter` - `autoconfigure.ddi.DdiApiAutoConfiguration`, `ControllerSecurityConfiguration`,
  `ControllerDownloadSecurityConfiguration`
- `hawkbit-dmf-starter` - `autoconfigure.dmf.amqp.DmfApiAutoConfiguration`
- `hawkbit-mcp-starter` - `autoconfigure.mcp.McpAutoConfiguration`
- `hawkbit-artifact-fs` - `autoconfigure.artifact.fs.FileArtifactStorageConfiguration`
- `hawkbit-repository-jpa-flyway` - `autoconfigure.repository.jpa.flyway.HawkbitFlywayAutoConfiguration`
