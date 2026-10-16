# hawkBit repository test

Test utilities for integration tests against the repository (used as test dependency by the repository, REST, DDI, DMF,
etc. modules).

## Content

- `TestConfiguration` - Spring test configuration, loads `hawkbit-test-defaults.properties` (lower quotas, disabled
  caches, artifact URL protocols, etc.)
- `util.AbstractIntegrationTest` - base class for integration tests: `@SpringBootTest` with profile `test`, default
  user `bumlux` (`TENANT_ADMIN`, `CONTROLLER_ROLE`, `SYSTEM_ROLE`), event verification and repository cleanup after each
  test, context destroyed after each test class. `hawkbit-repository-jpa` extends it with `AbstractJpaIntegrationTest`
- `util.TestdataFactory` - generator for test data (targets, distribution sets, software modules, rollouts, ...)
- `util.WithUser` - annotation to run a test class / method as a given user / tenant with given authorities
- `util.SecurityContextSwitch` - programmatic switch of the security context (`runAs`, `getAs`, `callAs`,
  `asPrivileged`, `withUser`, `withController`, ...)
- `matcher.ExpectEvents` / `matcher.EventVerifier` - declare and verify the events a test method is expected to fire
- `util.CleanupTestExecutionListener` - cleans the repository after each test method
- `util.QueryCount`, `util.QueryCountingDataSource` - JDBC level statement recorder (vendor independent) for
  performance / caching tests
- `util.SharedSqlTestDatabaseExtension`, `util.DisposableSqlTestDatabaseExtension` - JUnit extensions creating random
  test schemas (see below), with `H2TestDatabase`, `MySqlTestDatabase`, `PostgreSqlTestDatabase`

## Test database

By default tests run against an in-memory H2 database (`spring.jpa.database=H2` from the JPA defaults).

To run the tests against another database, pass all of `spring.jpa.database`, `spring.datasource.url`,
`spring.datasource.username` and `spring.datasource.password` as system properties (`DatasourceContext`; the
upper-case variants, e.g. `SPRING_JPA_DATABASE`, are accepted too). Supported values for `spring.jpa.database`: `H2`,
`MYSQL`, `POSTGRESQL`. For example (as in the PostgreSQL CI workflow):

```shell
mvn clean verify -Dspring.jpa.database=POSTGRESQL \
  -Dspring.datasource.url=jdbc:postgresql://localhost:5432/hawkbit \
  -Dspring.datasource.username=postgres -Dspring.datasource.password=admin
```

If all are set, `SharedSqlTestDatabaseExtension` (registered on `AbstractIntegrationTest`) creates a random schema
`HAWKBIT_TEST_<random>` once per JVM, points `spring.datasource.url` to it and drops it on JVM shutdown.
`DisposableSqlTestDatabaseExtension` creates a separate random schema for a test class and drops it after the class.
The prefix can be changed via the system property `spring.database.random.prefix`. If any of the four properties is
missing, no schema is created and the configured (default H2) datasource is used.
