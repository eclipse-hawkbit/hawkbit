hawkBit Docker
===

# Setup

## A: Docker Container

Start the hawkBit Update Server as a single container

```bash
$ docker run -d -p 8080:8080 hawkbit/hawkbit-update-server:latest
```

## B: Docker Compose

Start the hawkBit Update Server together with an MySQL and RabbitMQ instance as containers

```bash
# Requires Docker Compose to be installed
$ docker-compose up -d
```


## C: Docker Stack

Start the hawkBit Update Server and Device Simulator together with an MySQL and RabbitMQ instance as services within a swarm

```bash
$ docker swarm init
$ docker stack deploy -c docker-compose-stack.yml hawkbit
```

# Access

| Service / Container | URL | Login | A | B | C |
|---|---|---|---|---|---|
| hawkBit Update Server | [http://localhost:8080/](http://localhost:8080/) | admin:admin | &#10003; | &#10003; | &#10003; |
| hawkBit Device Simulator | [http://localhost:8083/](http://localhost:8083/) | - |  |  | &#10003; |
| MySQL | localhost:3306/hawkbit | root |  | &#10003; | &#10003; |
| RabbitMQ | [http://localhost:15672](http://localhost:15672) | guest:guest |  | &#10003; | &#10003; |

# Configuration

You can override application.properties by setting an environment variable SPRING_APPLICATION_JSON for hawkbit container.
```
hawkbit:
    image: "hawkbit/hawkbit-update-server:latest-mysql"
    environment:
      SPRING_APPLICATION_JSON: '{
        "spring.datasource.url": "jdbc:mysql://mysql:3306/hawkbit",
        "spring.rabbitmq.host": "rabbitmq",
        "spring.rabbitmq.username": "guest",
        "spring.rabbitmq.password": "guest",
        "spring.datasource.username": "root",
        "hawkbit.server.im.users[0].username": "hawkbit",
        "hawkbit.server.im.users[0].password": "{noop}isAwesome!",
        "hawkbit.server.im.users[0].firstname": "Eclipse",
        "hawkbit.server.im.users[0].lastname": "HawkBit",
        "hawkbit.server.im.users[0].permissions": "ALL"
      }'
```
