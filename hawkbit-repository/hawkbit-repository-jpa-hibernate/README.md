# hawkBit JPA Hibernate Vendor integration

Implementation of [Hibernate](https://hibernate.org/) JPA vendor.

Contains the vendor specific `JpaConfiguration`, `Jpa` helpers, `Statistics`, the tenant resolution (`TenantIdentifier`)
and the base entity classes (`model.AbstractJpaBaseEntity`, `model.AbstractJpaTenantAwareBaseEntity`,
`model.EntityPropertyChangeListener`). `JpaConfiguration`, `Jpa`, `Statistics` and the base entity classes exist with
the same names in `hawkbit-repository-jpa-eclipselink`, so exactly one of the two vendor modules must be on the
classpath.

The JPA vendor is selected at build time via the Maven property `jpa.vendor` (default `eclipselink`, defined in
`hawkbit-repository-jpa/pom.xml`):

- profile `eclipselink` - active when `jpa.vendor` is not `hibernate`; adds `hawkbit-repository-jpa-eclipselink` and
  the EclipseLink static weaving (`eclipselink-maven-plugin`)
- profile `hibernate` - active with `-Djpa.vendor=hibernate`; adds this module and the Hibernate bytecode enhancement
  (`hibernate-maven-plugin`, `enhance` goal)

To build hawkBit with Hibernate:

```shell
mvn clean install -Djpa.vendor=hibernate
```

## Lazy loading

Hibernate runs without `hibernate.enable_lazy_load_no_trans`: lazy relations (and not initialized lazy proxies) can be
loaded only within the session that loaded the entity, otherwise a `LazyInitializationException` is thrown. EclipseLink
loads them silently, so code that works with EclipseLink may fail with Hibernate. Rules:

- load what is used outside the transaction with an entity graph (e.g. `DistributionSetManagement#getWithDetails`,
  `DistributionSetManagement#findWithArtifacts`, `DistributionSetTypeManagement#getWithModuleTypes`), especially on
  event / AMQP listener threads that have no open session. Cached entities (by-id cache) are detached too
- a management method that gets an entity from the caller and navigates its lazy relations must use the managed
  instance (`AbstractJpaRepositoryManagement#managed`, `JpaManagementHelper#isDetached`)
- code run after commit (`AfterTransactionCommitExecutor`, e.g. event creation) must not navigate not initialized lazy
  relations - read them within the transaction (EclipseLink assigns ids of new entities on flush, so ids of new
  entities are available only after commit there)
- in tests, navigate lazy relations of returned entities within a transaction or re-read them with the needed graph
