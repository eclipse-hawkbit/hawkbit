# hawkBit JPA Flyway migration

JPA Flyway migrations scripts.

When this module is on the classpath of a Spring Boot application, `HawkbitFlywayAutoConfiguration` loads
`hawkbit-jpa-flyway-defaults.properties` (before Spring Boot's `FlywayAutoConfiguration`) and the schema is migrated at
application start. Alternatively, the schema can be migrated before hawkBit starts using `hawkbit-repository-jpa-init`.

## Defaults

| Property                                  | Value                        |
|-------------------------------------------|------------------------------|
| `spring.flyway.enabled`                   | `true`                       |
| `spring.flyway.clean-disabled`            | `true`                       |
| `spring.flyway.sql-migration-suffixes`    | `${spring.jpa.database}.sql` |
| `spring.flyway.ignore-migration-patterns` | `*:missing`                  |
| `spring.flyway.table`                     | `schema_version`             |

## Migration scripts

- Location: `src/main/resources/db/migration/{H2,MYSQL,POSTGRESQL}/`
- Naming: `V1_20_<n>__<description>__<DB>.sql`, e.g. `V1_20_5__tenant_lazy_delete__MYSQL.sql`
- Baseline: `B1_20_0__1.0.0_baseline__<DB>.sql` (full schema)
- The DB specific scripts are selected by the suffix `<DB>.sql`, where `<DB>` is the value of `spring.jpa.database`
  (`H2`, `MYSQL` - also used for MariaDB, `POSTGRESQL`)

Rules for schema changes:

- every schema change needs one script per DB (`H2`, `MYSQL`, `POSTGRESQL`) with the same version and description
- use the next free `<n>` - never change an already released script, add a new one instead
