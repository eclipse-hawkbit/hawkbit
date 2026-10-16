# Eclipse hawkBit

Domain-independent back end for rolling out software updates to IoT devices. It provides the Management REST API, the DDI device HTTP API, the DMF AMQP API, a Vaadin UI and an MCP server.
Stack: Java 21, Maven multi-module, Spring Boot 4, EclipseLink 5 (default) or Hibernate, Flyway, Vaadin 25. Version `0-SNAPSHOT` (`${revision}` + flatten plugin).

## Commands
There is no Maven wrapper, so use the system `mvn`.
- Build: `mvn clean install -DskipTests`
- Test all: `mvn clean verify`
- Test one: `mvn -pl <module-path> -am test -Dtest=ClassName[#method] -Dsurefire.failIfNoSpecifiedTests=false`
- Format: `mvn spotless:apply`. Check with `mvn spotless:check`. Only files changed since `origin/master` are checked (`-DratchetFrom=<ref>`).
- License headers: `mvn license:check -PcheckLicense` (templates in `licenses/`)
- Hibernate build/test: add `-Djpa.vendor=hibernate`
- PostgreSQL tests: add `-Dspring.jpa.database=POSTGRESQL -Dspring.datasource.url=jdbc:postgresql://localhost:5432/hawkbit -Dspring.datasource.username=postgres -Dspring.datasource.password=admin`
- Run monolith: `java -jar hawkbit-monolith/hawkbit-update-server/target/hawkbit-update-server-0-SNAPSHOT.jar` (port 8080, admin/admin)
- Run UI: `java -jar hawkbit-ui/target/hawkbit-ui.jar` (port 8088)
- Docker: see `docker/README.md` (`docker compose -f mysql/docker-compose-monolith-mysql.yml up`)

CI (`.github/workflows/reusable_workflow_verify.yaml`) runs `license:check`, then `mvn clean verify javadoc:javadoc -PgenerateTestReport` against H2 or PostgreSQL × EclipseLink or Hibernate. `style_check.yaml` runs spotless.

## Layout
- `hawkbit-core/`: shared infra: `auth` (SpPermission, SpRole, SpringEvalExpressions), `context` (AccessContext), `tenancy`, `throttle`, `audit`, exceptions
- `hawkbit-ql-jpa/`: RSQL → JPA Specification
- `hawkbit-artifact/`: artifact storage API and file-system implementation
- `hawkbit-repository/`: domain model and services (`*-api` interfaces, `*-jpa` implementation, vendor modules, Flyway, test utils). See `hawkbit-repository/AGENTS.md`
- `hawkbit-rest/`: shared REST base (OpenAPI, error handling, DosFilter)
- `hawkbit-autoconfigure/`: Spring Boot auto-configurations (security, throttle, executors, repository)
- `hawkbit-mgmt/`, `hawkbit-ddi/`, `hawkbit-dmf/`: the three APIs, each split as `-api` / `-resource` (or `-amqp`) / `-starter` / `-server`
- `hawkbit-monolith/`: `hawkbit-starter` (all APIs) and `hawkbit-update-server` (all-in-one app)
- `hawkbit-mcp/`: MCP server over the Mgmt API. See `hawkbit-mcp/AGENTS.md`
- `hawkbit-ui/`: Vaadin Flow UI, talks to Mgmt REST. See `hawkbit-ui/AGENTS.md`
- `hawkbit-sdk/`: Feign-based Java clients (mgmt, device, dmf) and demo apps
- `hawkbit-test-report/`: aggregated JaCoCo report, only with `-PgenerateTestReport`
- `docker/`: compose files (`mysql/`, `postgres/`) and image build (`docker/build/`)
- `docs/`: user docs (Docsify, nav in `docs/_sidebar.md`)

## Conventions
- **API layering:** `*-api` holds `*RestApi` interfaces (Spring MVC and springdoc annotations) and `json/model` DTOs. `*-resource` holds the implementations (`MgmtXxxResource implements MgmtXxxRestApi`). SDK, UI and MCP consume the `-api` interfaces via Feign, so an interface change affects all clients.
- **Services:** `XxxManagement` (repository-api) → `JpaXxxManagement` (repository-jpa `management`).
  - `get…` returns the entity or throws `EntityNotFoundException`. `find…` returns `Optional`.
  - Secure methods with `@PreAuthorize(SpringEvalExpressions.…)`. Permissions are String constants in `SpPermission`.
- **Tenancy/security context:** use the static `AccessContext` (`tenant()`, `actor()`, `asTenant`, `asActor`, `asSystem`, `asSystemAsTenant`). Don't read the `SecurityContext` directly.
- **Exceptions:** extend `AbstractServerRtException` with an `SpServerError` code. HTTP mapping is in rest-core `RestConfiguration`.
- **Events:** publish via `EventPublisherHolder`, after commit (`AfterTransactionCommitExecutor`). Remote events (`hawkbit.events.remote.enabled`) default to `true`; the monolith turns them off.
- **Backward compatibility:** Mgmt `/rest/v1` and DDI v1 must stay compatible. Deprecate (`@Deprecated`) instead of removing or renaming fields and endpoints.
- **Code style:**
  - Lombok (`@Slf4j`, builders); `final` parameters.
  - No new utility libraries: prefer JDK, then Spring, then commons-lang (`CONTRIBUTING.md`).
  - Every file needs the EPL license header.
- **Tests:**
  - JUnit 5 + Spring Boot tests on H2. Class names end in `*Test` (surefire only, no failsafe, no Testcontainers).
  - Extend the matching base class: `AbstractJpaIntegrationTest`, `AbstractManagementApiIntegrationTest`, `AbstractDDiApiIntegrationTest`, `AbstractAmqpIntegrationTest`.
  - Use `@WithUser`, `TestdataFactory`, `@ExpectEvents`.
  - Document the test class with Javadoc `Feature: <Type> - <Component><br/>` and `Story: …` (see `CONTRIBUTING.md`).

## Gotchas
- **JPA vendor is chosen at build time** (`jpa.vendor` property, Maven profiles in `hawkbit-repository-jpa/pom.xml`). Base entities exist in both `-jpa-eclipselink` and `-jpa-hibernate`, so change both.
- **Generated code, don't edit:**
  - JPA metamodel `*_` classes in `target/generated-sources/apt/`
  - EclipseLink static weaving / Hibernate enhancement output
  - Vaadin frontend files in `hawkbit-ui`
- **Schema change:** add one Flyway script per DB (H2, MYSQL, POSTGRESQL) with the same version. See `hawkbit-repository/hawkbit-repository-jpa-flyway/README.md`.
- **Schedulers** (rollout, auto-assign, auto-cleanup, tenant hard-delete) are off by default. They are enabled via `hawkbit.<name>.scheduler.enabled`, and only mgmt-server and the monolith set them.
- **Monolith defaults:** RabbitMQ and DMF are off (`hawkbit.dmf.enabled=false`, Rabbit autoconfig excluded).
- **AMQP tests** need RabbitMQ on localhost:5672 with the management plugin on :15672 (guest/guest). Without it they are skipped.
- **Default ports:**
  - mgmt-server, monolith: 8080 (they clash if run together); dmf-server has no HTTP port (`web-application-type=none`)
  - ddi-server: 8081
  - mcp-server: 8082
  - ui: 8088
- `docs/rest-api/*.html` are pre-rendered Redoc bundles, regenerated by hand from `/v3/api-docs`. Don't edit them.
- The `!…/json/model/target` exception in `.gitignore` is intentional: the `target` package is source, not build output.

## Read when
- Running/deploying, quick start → `docs/quick-start.md`, `docker/README.md`
- DB, Flyway, JPA vendor → `docs/database-setup.md`, `hawkbit-repository/AGENTS.md`
- Security, permissions, users, OIDC, access control → `docs/authorization.md`, `docs/authentication.md`
- Clustering, caches, remote events → `docs/clustering.md`
- Throttling / DoS filter → `docs/throttling.md`
- Rollouts → `docs/rollout-management.md`, `hawkbit-repository/README.md`
- Entities / data model → `docs/datamodel.md`, `docs/entity-definitions.md`
- RSQL query syntax → `docs/query-parameters.md`, `hawkbit-ql-jpa/`
- DMF message format → `docs/device-management-federation-api.md`
- SDK / Feign clients → `docs/hawkbit-sdk.md`, `docs/feign-client.md`, `hawkbit-sdk/README.md`
- Module internals → the module's `README.md`
- Contributing rules, code style, test docs → `CONTRIBUTING.md`

## Definition of done
- Tests added/updated and passing: `mvn -pl <module> -am verify`
- Formatted: `mvn spotless:apply`. License headers present: `mvn license:check -PcheckLicense`
- A schema change has scripts for all three DBs, and an entity change is applied in both vendor modules
- Docs in sync (below)

## Keeping docs in sync
Update these in the same change set:
- Commands, layout or conventions changed → this file / nearest module `AGENTS.md`
- Build, run or config changed → relevant `README.md`
- User-visible behavior, config options or concepts changed → `docs/` (add new pages to `docs/_sidebar.md`)
- Entity fields changed → `docs/entity-definitions.md` and `hawkbit-mcp/hawkbit-mcp-starter/src/main/resources/hawkbit-entity-definitions.md`

Keep this file short: link, don't duplicate.
