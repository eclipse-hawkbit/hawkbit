# Quick Start

---

## From Docker Image

#### Overview
HawkBit Update Server default user has **admin/admin** as default credentials.  
See below how the user can be changed.

It supports two configurations:

- **monolith** – `hawkbit-update-server`  
- **micro-service** – `hawkbit-mgmt-server`, `hawkbit-ddi-server`, `hawkbit-dmf-server`  

---

#### A: Run hawkBit Update Server (Monolith) as Docker Container

Start the hawkBit Update Server as a single container:

```bash
$ docker run -p 8080:8080 hawkbit/hawkbit-update-server:latest
```

This will start hawkBit update server with an embedded H2 database for evaluation purposes.

---

#### B: Run hawkBit Update Server (Monolith) with services as Docker Compose

Start the hawkBit Update Server together with a PostgreSQL and RabbitMQ instance as containers:

```bash
$ git clone https://github.com/eclipse-hawkbit/hawkbit.git
$ cd hawkbit/docker
$ docker-compose -f docker-compose-monolith-postgres.yml up -d
```

or with MySQL:

```bash
$ git clone https://github.com/eclipse-hawkbit/hawkbit.git
$ cd hawkbit/docker
$ docker-compose -f docker-compose-monolith-mysql.yml up -d
```

If you want to start also the hawkBit UI, you can use, for PostgreSQL:

```bash
$ git clone https://github.com/eclipse-hawkbit/hawkbit.git
$ cd hawkbit/docker
$ docker-compose -f docker-compose-monolith-with-ui-postgres.yml up -d
```

or with MySQL:

```bash
$ git clone https://github.com/eclipse-hawkbit/hawkbit.git
$ cd hawkbit/docker
$ docker-compose -f docker-compose-monolith-with-ui-mysql.yml up -d
```

> **Note:** `-d` flag is used to run the containers in detached mode.  
> If you want to see the logs, you can remove the flag.  

---

#### C: Run hawkBit Update Server (Micro-Service) with services as Docker Compose

Start the hawkBit Update Server together with a PostgreSQL and RabbitMQ instance as containers:

```bash
$ git clone https://github.com/eclipse-hawkbit/hawkbit.git
$ cd hawkbit/docker
$ docker-compose -f docker-compose-micro-services-postgres.yml up -d
```

or with MySQL:

```bash
$ git clone https://github.com/eclipse-hawkbit/hawkbit.git
$ cd hawkbit/docker
$ docker-compose -f docker-compose-micro-services-mysql.yml up -d
```

If you want to start also the hawkBit UI, you can use, for PostgreSQL:

```bash
$ git clone https://github.com/eclipse-hawkbit/hawkbit.git
$ cd hawkbit/docker
$ docker-compose -f docker-compose-micro-services-with-ui-postgres.yml up -d
```

or with MySQL:

```bash
$ git clone https://github.com/eclipse-hawkbit/hawkbit.git
$ cd hawkbit/docker
$ docker-compose -f docker-compose-micro-services-with-ui-mysql.yml up -d
```

> **Note:** `-d` flag is used to run the containers in detached mode.  
> If you want to see the logs, you can remove the flag.  

---

## From Sources

#### 1: Clone and build hawkBit

```bash
$ git clone https://github.com/eclipse-hawkbit/hawkbit.git
$ cd hawkbit
$ mvn clean install -DskipTests
```

---

#### 2: Start hawkBit &nbsp; [Update Server](https://github.com/eclipse-hawkbit/hawkbit/tree/master/hawkbit-monolith/hawkbit-update-server) (Monolith)

```bash
$ java -jar ./hawkbit-monolith/hawkbit-update-server/target/hawkbit-update-server-0-SNAPSHOT.jar
```

**Note:** you could start it also in **microservices mode** by:

```bash
$ java -jar ./hawkbit-mgmt/hawkbit-mgmt-server/target/hawkbit-mgmt-server-0-SNAPSHOT.jar
$ java -jar ./hawkbit-ddi/hawkbit-ddi-server/target/hawkbit-ddi-server-0-SNAPSHOT.jar
$ java -jar ./hawkbit-dmf/hawkbit-dmf-server/target/hawkbit-dmf-server-0-SNAPSHOT.jar
```

And (only if you want to use the DMF feature):

```bash
$ java -jar ./hawkbit-monolith/hawkbit-update-server/target/hawkbit-update-server-0-SNAPSHOT.jar
```

**Note:** You could also start with H2 console enabled with:
```bash
$java -jar ./hawkbit-monolith/hawkbit-update-server/target/hawkbit-update-server-0-SNAPSHOT.jar --spring.h2.console.enabled=true --spring.h2.console.path=/h2-console
```
for monolith, and:
```bash
$java -jar ./hawkbit-mgmt/hawkbit-mgmt-server/target/hawkbit-mgmt-server-0-SNAPSHOT.jar --spring.h2.console.enabled=true --spring.h2.console.path=/h2-console
```
for mgmt server in micro-service mode.

Then you will get H2 console available at '/h2-console' (Database available at 'jdbc:h2:mem:hawkbit')

You could also start the **hawkBit UI** by:

```bash
$ java -jar ./hawkbit--ui/target/hawkbit-ui-0-SNAPSHOT.jar
```

---

## Configuration

#### Change credentials

As stated before, the default user is **admin/admin**.  
It can be overridden by changing the [`TenantAwareUserProperties`](https://github.com/eclipse-hawkbit/hawkbit/blob/master/hawkbit-core/src/main/java/org/eclipse/hawkbit/tenancy/TenantAwareUserProperties.java) configuration using Spring.  

For instance, using a properties file like:

```properties
# should remove the admin/admin user
hawkbit.security.user.admin.tenant=#{null}
hawkbit.security.user.admin.password=#{null}
hawkbit.security.user.admin.roles=#{null}

# should add a hawkbit/isAwesome! user
hawkbit.security.user.hawkbit.tenant=DEFAULT
hawkbit.security.user.hawkbit.password={noop}isAwesome!
hawkbit.security.user.hawkbit.roles=TENANT_ADMIN
```

This will remove the default `admin/admin` user and add a user `hawkbit` with password `isAwesome!` and role `TENANT_ADMIN`.

You can create multiple users with specified roles.
