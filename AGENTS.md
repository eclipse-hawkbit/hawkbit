# Eclipse hawkBit

Domain-independent back end for rolling out software updates to IoT devices. It provides the Management REST API, the
DDI device HTTP API, the DMF AMQP API, a Vaadin UI and an MCP server.
Stack: Java 25, Maven multi-module, Spring Boot 4, EclipseLink 5 (default) or Hibernate, Flyway, Vaadin 25. Version
`0-SNAPSHOT` (`${revision}` + flatten plugin).

## Commands

There is no Maven wrapper, so use the system `mvn`.

- Build: `mvn clean install -DskipTests`
- Test all: `mvn clean verify`
- Test one: `mvn -pl <module-path> -am test -Dtest=ClassName[#method] -Dsurefire.failIfNoSpecifiedTests=false`
- Format check: `mvn validate -PideaFormat`. Format: `mvn validate -PideaFormat -Didea.format.args=--format`. Only
  files changed since the merge base with `origin/master`, in the dir maven is started in (root, module dir or
  `-f <module>`); more options in `-Didea.format.args` (`--base <ref>`, `--all`, paths). All file types (excludes:
  `DO_NOT_FORMAT` in `intellij_codeformatter.xml`). Needs a local IntelliJ IDEA: `-Didea.binary=<launcher>` if `idea`
  isn't on `PATH`. Runs `scripts/IdeaFormat` (Java single-file program); `.mvn/` only marks the root for maven.
- License headers: `mvn license:check -PcheckLicense`, add missing with `license:format`. One template
  (`licenses/LICENSE_HEADER_TEMPLATE.txt`); year and holder are `headerSections` in the root `pom.xml` (new holders go
  into the `__HOLDER__` regex).
- Hibernate build/test: add `-Djpa.vendor=hibernate`
- PostgreSQL tests: add
  `-Dspring.jpa.database=POSTGRESQL -Dspring.datasource.url=jdbc:postgresql://localhost:5432/hawkbit -Dspring.datasource.username=postgres -Dspring.datasource.password=admin`
- Run monolith: `java -jar hawkbit-monolith/hawkbit-update-server/target/hawkbit-update-server-0-SNAPSHOT.jar` (port
  8080, admin/admin)
- Run UI: `java -jar hawkbit-ui/target/hawkbit-ui-0-SNAPSHOT.jar` (port 8088)
- Docker: see `docker/README.md` (`docker compose -f mysql/docker-compose-monolith-mysql.yml up`)

CI (`.github/workflows/reusable_workflow_verify.yaml`) runs `license:check`, then
`mvn clean verify javadoc:javadoc -PgenerateTestReport` against H2 or PostgreSQL × EclipseLink or Hibernate.
`style_check.yaml` runs the format check (`scripts/IdeaFormat`).

## Layout

- `hawkbit-core/`: shared infra: `auth` (SpPermission, SpRole, SpringEvalExpressions), `context` (AccessContext),
  `tenancy`, `throttle`, `audit`, exceptions
- `hawkbit-ql-jpa/`: RSQL → JPA Specification
- `hawkbit-artifact/`: artifact storage API and file-system implementation
- `hawkbit-repository/`: domain model and services (`*-api` interfaces, `*-jpa` implementation, vendor modules, Flyway,
  test utils). See `hawkbit-repository/AGENTS.md`
- `hawkbit-rest/`: shared REST base (OpenAPI, error handling, DosFilter)
- `hawkbit-autoconfigure/`: Spring Boot auto-configurations (security, throttle, executors, repository)
- `hawkbit-mgmt/`, `hawkbit-ddi/`, `hawkbit-dmf/`: the three APIs, each split as `-api` / `-resource` (or `-amqp`) /
  `-starter` / `-server`
- `hawkbit-monolith/`: `hawkbit-starter` (all APIs) and `hawkbit-update-server` (all-in-one app)
- `hawkbit-mcp/`: MCP server over the Mgmt API. See `hawkbit-mcp/AGENTS.md`
- `hawkbit-ui/`: Vaadin Flow UI, talks to Mgmt REST. See `hawkbit-ui/AGENTS.md`
- `hawkbit-sdk/`: Feign-based Java clients (mgmt, device, dmf) and demo apps
- `hawkbit-test-report/`: aggregated JaCoCo report, only with `-PgenerateTestReport`
- `docker/`: compose files (`mysql/`, `postgres/`) and image build (`docker/build/`)
- `scripts/`: dev / CI scripts (`IdeaFormat`)
- `docs/`: user docs (Docsify, nav in `docs/_sidebar.md`)

## Conventions

- **API layering:** `*-api` holds `*RestApi` interfaces (Spring MVC and springdoc annotations) and `json/model` DTOs.
  `*-resource` holds the implementations (`MgmtXxxResource implements MgmtXxxRestApi`). SDK, UI and MCP consume the
  `-api` interfaces via Feign, so an interface change affects all clients.
- **Endpoint permissions:** annotate each Mgmt `@*Mapping` method with `@RequiredPermissions({ ApiPermission.X })`
  (`hawkbit-rest-api`; derive from the service `@PreAuthorize`, `${permissionGroup}` resolved per service). The OpenAPI
  text is generated from it, so don't write "Required Permission" in descriptions. `MgmtRequiredPermissionsTest`
  enforces presence, and `ApiPermissionTest` keeps `ApiPermission` in sync with `SpPermission`. Use `anyOf` for `A or B`
  checks and `optional` for conditional ones. Mgmt tests run with `MgmtRequiredPermissionsVerifier` on
  (`hawkbit.server.mgmt.permission-tracking.enabled`), which fails a test when a granted permission isn't declared.
- **Services:** `XxxManagement` (repository-api) → `JpaXxxManagement` (repository-jpa `management`).
    - `get…` returns the entity or throws `EntityNotFoundException`. `find…` returns `Optional`.
    - Secure methods with `@PreAuthorize(SpringEvalExpressions.…)`. Permissions are String constants in `SpPermission`.
- **Tenancy/security context:** use the static `AccessContext` (`tenant()`, `actor()`, `asTenant`, `asActor`,
  `asSystem`, `asSystemAsTenant`). Don't read the `SecurityContext` directly.
- **Exceptions:** extend `AbstractServerRtException` with an `SpServerError` code. HTTP mapping is in rest-core
  `RestConfiguration`.
- **Events:** publish via `EventPublisherHolder`, after commit (`AfterTransactionCommitExecutor`). Remote events
  (`hawkbit.events.remote.enabled`) default to `true`; the monolith turns them off.
- **Backward compatibility:** Mgmt `/rest/v1` and DDI v1 must stay compatible. Deprecate (`@Deprecated`) instead of
  removing or renaming fields and endpoints.
- **Code style:**
    - Lombok (`@Slf4j`, builders); `final` parameters.
    - No new utility libraries: prefer JDK, then Spring, then commons-lang (`CONTRIBUTING.md`).
    - Every file needs the EPL license header.
- **Tests:**
    - JUnit 5 + Spring Boot tests on H2. Class names end in `*Test` (surefire only, no failsafe, no Testcontainers).
    - Extend the matching base class: `AbstractJpaIntegrationTest`, `AbstractManagementApiIntegrationTest`,
      `AbstractDDiApiIntegrationTest`, `AbstractAmqpIntegrationTest`.
    - Use `@WithUser`, `TestdataFactory`, `@ExpectEvents`.
    - Document the test class with Javadoc `Feature: <Type> - <Component><br/>` and `Story: …` (see `CONTRIBUTING.md`).

## Gotchas

- **JPA vendor is chosen at build time** (`jpa.vendor` property, Maven profiles in `hawkbit-repository-jpa/pom.xml`).
  Base entities exist in both `-jpa-eclipselink` and `-jpa-hibernate`, so change both.
- **Generated code, don't edit:**
    - JPA metamodel `*_` classes in `target/generated-sources/apt/`
    - EclipseLink static weaving / Hibernate enhancement output
    - Vaadin frontend files in `hawkbit-ui`
- **RSQL field names** are the `qfields` enum constant names (case-insensitive, e.g. `assignedDS.name`, `tag.id`), not
  JPA field names. A field can be queried without a sub-attribute (`tag==x` → `tag.name`) only if it has exactly one
  sub-attribute or overrides `getDefaultSubEntityAttribute()`. Adding a second sub-attribute drops that default unless
  you override it, and that breaks stored filters, rollouts and auto-assignments.
- **Schema change:** add one Flyway script per DB (H2, MYSQL, POSTGRESQL) with the same version. See
  `hawkbit-repository/hawkbit-repository-jpa-flyway/README.md`.
- **Schedulers** (rollout, auto-assign, auto-cleanup, tenant hard-delete) are off by default. They are enabled via
  `hawkbit.<name>.scheduler.enabled`, and only mgmt-server and the monolith set them.
- **Monolith defaults:** RabbitMQ and DMF are off (`hawkbit.dmf.enabled=false`, Rabbit autoconfig excluded).
- **AMQP tests** need RabbitMQ on localhost:5672 with the management plugin on :15672 (guest/guest). Without it they are
  skipped.
- **Default ports:**
    - mgmt-server, monolith: 8080 (they clash if run together); dmf-server has no HTTP port
      (`web-application-type=none`)
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
- Formatted: `mvn validate -PideaFormat` passes. License headers present: `mvn license:check -PcheckLicense`
- A schema change has scripts for all three DBs, and an entity change is applied in both vendor modules
- Docs in sync (below)

## Keeping docs in sync

Update these in the same change set:

- Commands, layout or conventions changed → this file / nearest module `AGENTS.md`
- Build, run or config changed → relevant `README.md`
- User-visible behavior, config options or concepts changed → `docs/` (add new pages to `docs/_sidebar.md`)
- Entity fields changed → `docs/entity-definitions.md` and
  `hawkbit-mcp/hawkbit-mcp-starter/src/main/resources/hawkbit-entity-definitions.md`
- Query fields (`hawkbit-repository-api/.../qfields`) changed → field tables in `docs/entity-definitions.md` and the MCP
  copy

Keep this file short: link, don't duplicate.
