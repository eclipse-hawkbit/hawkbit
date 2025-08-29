hawkBit Docker
===

## Overview
This folder contains example Docker build and Docker Compose files to build and start the hawkBit as monolith or as microservices.

## Build
You could build the hawkbit Docker images following the [README.md](build/README.md) instructions.

## Start
You can start hawkbit as a Docker Container (only monolith) or with Docker Compose

#### A: Docker Container (only as monolith)
_Note: You need to have Docker installed on your machine._

Start the hawkBit Update Server (monolith) as a single container (with embedded H2, if you configure a different database, e.g. MySQL or PostgreSQL, you should start it separately):

```bash
$ docker run -d -p 8080:8080 hawkbit/hawkbit-update-server:latest
```

### B: Docker Compose
_Note: You need to have Docker Compose installed on your machine._

Start the hawkBit Update Server (monolith) together with an MySQL and RabbitMQ instance as containers (Requires Docker Compose to be installed)

```bash
$ docker compose -f mysql/docker-compose-monolith-mysql.yml up
```
With the upper command CTRL+C shuts down all services. Add '-d' at the end to start all into detached mode:
```bash
$ docker compose -f mysql/docker-compose-monolith-mysql.yml up -d
```
Then stop all services with:
```bash
$ docker compose -f mysql/docker-compose-monolith-mysql.yml down
```

You could, also start it in different flavours, with UI or in microservices mode (see Docker Compose files in [mysql](./mysql) and [postgres](./postgres) folders). For instance to start with PostgreSQL, with RabbitMQ, in microservices mode and with UI you could use:
```bash
$ docker compose -f postgres/docker-compose-micro-services-with-simple-ui-postgres.yml up
```

### Access
| Service / Container   | URL                    | Login       | A        | B        |
|-----------------------|------------------------|-------------|----------|----------|
| hawkBit Update Server | [http://localhost:8080/](http://localhost:8080/) | admin:admin | &#10003; | &#10003; |
| MySQL                 | localhost:3306/hawkbit | root        |          | &#10003; |
| RabbitMQ              | [http://localhost:15672](http://localhost:15672) | guest:guest |          | &#10003; |

### Configuration
You can override _application.properties_ by setting an environment variable _SPRING_APPLICATION_JSON_ to the hawkbit container, e.g.:

```yaml
hawkbit:
    image: "hawkbit/hawkbit-update-server:latest"
    environment:
      SPRING_APPLICATION_JSON: '{
        "spring.datasource.url": "jdbc:mariadb://mysql:3306/hawkbit",
        "spring.rabbitmq.host": "rabbitmq",
        "spring.rabbitmq.username": "guest",
        "spring.rabbitmq.password": "guest",
# should remove default admin/admin user
        "hawkbit.security.user.admin.tenant": "#{null}",  
        "hawkbit.security.user.admin.password": "#{null}",  
        "hawkbit.security.user.admin.roles": "#{null}",
# should add hawkbit/isAwesome! user        
        "hawkbit.security.user.hawkbit.tenant": "DEFAULT",
        "hawkbit.security.user.hawkbit.password": "{noop}isAwesome!",
        "hawkbit.security.user.hawkbit.roles": "TENANT_ADMIN" 
      }'
```