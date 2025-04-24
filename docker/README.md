hawkBit Docker
===

# Setup

## A: Docker Container
Start the hawkBit Update Server as a single container (requires Docker to be installed and all dependencies to be available)

```bash
$ docker run -d -p 8080:8080 hawkbit/hawkbit-update-server:latest
```

## B: Docker Compose
Start the hawkBit Update Server together with an MySQL and RabbitMQ instance as containers (Requires Docker Compose to be installed)

```bash
$ docker compose -f mysql/docker-compose-monolith-mysql.yml up
```
You could, also start it in different flavours, with UI or in microservices mode.

Note: Whit the upper command CTRL+C shuts down all services. Add '-d' at the end to start all into detached mode:
```bash
$ docker compose -f mysql/docker-compose-monolith-mysql.yml up -d
```
Then stop all services with:
```bash
$ docker compose -f mysql/docker-compose-monolith-mysql.yml down
```

# Access
| Service / Container      | URL                                              | Login       | A        | B        | C        |
|--------------------------|--------------------------------------------------|-------------|----------|----------|----------|
| hawkBit Update Server    | [http://localhost:8080/](http://localhost:8080/) | admin:admin | &#10003; | &#10003; | &#10003; |
| MySQL                    | localhost:3306/hawkbit                           | root        |          | &#10003; | &#10003; |
| RabbitMQ                 | [http://localhost:15672](http://localhost:15672) | guest:guest |          | &#10003; | &#10003; |

# Configuration
You can override application.properties by setting an environment variable SPRING_APPLICATION_JSON for hawkbit container.

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
