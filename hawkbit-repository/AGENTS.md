# hawkbit-repository

Domain model and services: `-api` holds entity interfaces, `*Management` services, events, exceptions and tenant config.
`-jpa` is the implementation. The vendor modules add EclipseLink/Hibernate specifics. `-jpa-flyway` holds migrations,
`-jpa-init` is the standalone DB init jar, and `-test` holds test utilities.

## Commands

- Test the JPA module: `mvn -pl hawkbit-repository/hawkbit-repository-jpa -am test`
- With Hibernate: add `-Djpa.vendor=hibernate`. Default is `eclipselink` (`hawkbit-repository-jpa/pom.xml`).

## Rules

- **New service method:**
    - Declare it in `-api` `XxxManagement` with `@PreAuthorize(SpringEvalExpressions.…)`, then implement it in `-jpa`
      `management/JpaXxxManagement`.
    - `get…` throws `EntityNotFoundException`. `find…` returns `Optional`.
- **Entity changes:**
    - Add a migration (below).
    - If the base entity classes are touched (`AbstractJpaBaseEntity`, `AbstractJpaTenantAwareBaseEntity`), change both
      `-jpa-eclipselink` and `-jpa-hibernate`; each has its own copy.
    - Update `../docs/entity-definitions.md` and the MCP copy (see root `AGENTS.md`).
    - Tables use the `sp_` prefix.
- **Migrations:** `-jpa-flyway/src/main/resources/db/migration/{H2,MYSQL,POSTGRESQL}/V1_20_<n>__<desc>__<DB>.sql`.
    - Use one file per DB, all with the same version. Never edit released scripts.
    - MariaDB uses the MYSQL scripts.
- **Generated, don't edit:** `target/generated-sources/apt/` (JPA metamodel `*_`) and the woven/enhanced classes.
    - EclipseLink static weaving needs `FakeJpaEntityForStaticCompilation`; keep it.
- **Events** are published after commit via `AfterTransactionCommitExecutor`. In tests, assert them with
  `@ExpectEvents`.
- **Schedulers** under `-jpa/.../scheduler` are created only when `hawkbit.<name>.scheduler.enabled=true`. The rollout
  scheduler takes a per-tenant lock from `LockRegistry` (`hawkbit.lock`, `hawkbit.repository.cluster.lock.*`).
- **Access control:** entity-level filtering lives in `-jpa/.../acm` (`AccessController`). Respect it in new
  queries/specifications.
- **Tests:**
    - Extend `AbstractJpaIntegrationTest` (in `-jpa`) or `AbstractIntegrationTest` (in `-test`). The DB is H2.
    - For MySQL/PostgreSQL, pass `-Dspring.jpa.database=…` plus the datasource properties as system properties (see
      `hawkbit-repository-test/README.md`).

## Read when

- Rollout concepts and scheduler → `README.md`
- Service naming conventions → `hawkbit-repository-api/README.md`
- Migrations and Flyway defaults → `hawkbit-repository-jpa-flyway/README.md`
- Switching vendor → `hawkbit-repository-jpa-hibernate/README.md`, `../docs/database-setup.md`
- Test DB setup → `hawkbit-repository-test/README.md`
