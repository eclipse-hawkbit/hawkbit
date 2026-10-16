# hawkBit JPA Hibernate Vendor integration

Implementation of [Hibernate](https://hibernate.org/) JPA vendor.

Contains the vendor specific `JpaConfiguration`, `Jpa` helpers, `Statistics`, the tenant resolution (`TenantIdentifier`)
and the base entity classes (`model.AbstractJpaBaseEntity`, `model.AbstractJpaTenantAwareBaseEntity`,
`model.EntityPropertyChangeListener`). `JpaConfiguration`, `Jpa`, `Statistics` and the base entity classes exist with
the same names in `hawkbit-repository-jpa-eclipselink`, so exactly one of the two vendor modules must be on the classpath.

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
