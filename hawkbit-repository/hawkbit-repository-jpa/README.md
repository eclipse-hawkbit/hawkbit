# hawkBit JPA implementation

JPA implementation of the repository. Based on [EclipseLink](http://www.eclipse.org/eclipselink/) (default)
and [Spring Data Jpa](http://projects.spring.io/spring-data-jpa/). [Hibernate](https://hibernate.org/) could be used
alternatively - the vendor is selected at build time via `-Djpa.vendor=hibernate`
(see [hawkbit-repository-jpa-hibernate](../hawkbit-repository-jpa-hibernate/README.md)).

Entry point is `JpaRepositoryConfiguration`, defaults are in `hawkbit-jpa-defaults.properties` (H2 in-memory DB).

## Key packages (`org.eclipse.hawkbit.repository.jpa.*`)

- `model` - JPA entities (`Jpa*`) and abstract base entities
- `management` - `Jpa*Management` implementations of the management APIs from `hawkbit-repository-api`
- `repository` - Spring Data repositories (`*Repository`, `HawkbitBaseRepository`, `BaseEntityRepository`)
- `specifications` - Spring Data JPA `Specification`s used by the managements
- `rollout` - rollout group success / error conditions and actions (`rollout.condition`)
- `scheduler` - rollout and auto assignment schedulers and handlers (`JpaRolloutHandler`, `JpaRolloutExecutor`,
  `JpaAutoAssignHandler`), tenant hard delete scheduler
- `acm` - access control (`AccessController`, `DefaultAccessController`)
- `cluster` - distributed lock repository (`DistributedLockRepository`, `LockProperties`)
- `autocleanup` - auto cleanup scheduler and cleanup tasks (`AutoActionCleanup`)
- `utils`, `aspects`, `event` - helpers, exception mapping aspect, event entity manager

## Schedulers

All schedulers are **disabled by default** and must be enabled explicitly (the mgmt server and the monolith enable all
of them in their default `application.properties`). They are never created in the `test` profile.

| Scheduler                   | Enable property                                | Delay property (ms)                               | Default delay      |
|-----------------------------|------------------------------------------------|---------------------------------------------------|--------------------|
| `RolloutScheduler`          | `hawkbit.rollout.scheduler.enabled`            | `hawkbit.rollout.scheduler.fixedDelay`            | `2000`             |
| `AutoAssignScheduler`       | `hawkbit.autoassign.scheduler.enabled`         | `hawkbit.autoassign.scheduler.fixedDelay`         | `2000`             |
| `AutoCleanupScheduler`      | `hawkbit.autocleanup.scheduler.enabled`        | `hawkbit.autocleanup.scheduler.fixedDelay`        | `86400000` (1 day) |
| `TenantHardDeleteScheduler` | `hawkbit.tenant.hard-delete.scheduler.enabled` | `hawkbit.tenant.hard-delete.scheduler.fixedDelay` | `3600000` (1 hour) |

The rollout and auto assignment schedulers process the tenants sequentially by default; with
`hawkbit.rollout.executor.thread-pool.size` / `hawkbit.autoassign.executor.thread-pool.size` greater than `1`
(default `1`) tenants are processed in parallel by a thread pool of that size.

To avoid concurrent processing on multiple nodes, the rollout and auto assignment handlers, auto cleanup and tenant hard
delete use locks
from the `LockRegistry`: by default (`hawkbit.lock=distributed`) a JDBC based registry (`DistributedLockRepository`,
configured via `hawkbit.repository.cluster.lock.*`), otherwise an in-memory one.

## Tests

Integration tests extend `AbstractJpaIntegrationTest` (based on `AbstractIntegrationTest` from
[hawkbit-repository-test](../hawkbit-repository-test/README.md)) and use `jpa-test.properties`. See there how to run
the tests against MySQL / PostgreSQL.
