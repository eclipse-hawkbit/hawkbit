# hawkBit JPA Hibernate Vendor integration

Implementation of [Hibernate](https://hibernate.org/) JPA vendor.

To use this vendor you could exclude the org.eclipse.hawkbit:hawkbit.repository-jpa-eclipselink and include this module. 
For instance if you use org.eclipse.hawkbit:hawkbit-repository-jpa via org.eclipse.hawkbit:hawkbit-starter you could do it like this:

```xml
<dependency>
    <groupId>org.eclipse.hawkbit</groupId>
    <artifactId>hawkbit-starter</artifactId>
    <version>${project.version}</version>
    <exclusions>
        <exclusion>
            <groupId>org.eclipse.hawkbit</groupId>
            <artifactId>hawkbit-repository-jpa-eclipselink</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.eclipse.hawkbit</groupId>
    <artifactId>hawkbit-repository-jpa-hibernate</artifactId>
    <version>${project.version}</version>
</dependency>
```