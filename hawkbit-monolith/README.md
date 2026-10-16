hawkBit Runtime
===

| Module                   | Description                                                                                                  |
|--------------------------|--------------------------------------------------------------------------------------------------------------|
| `hawkbit-starter/`       | Spring Boot starter bundling the Management, DDI and DMF starters (all hawkBit interfaces in one).          |
| `hawkbit-update-server/` | The hawkBit Update Server - monolith Spring Boot application (`org.eclipse.hawkbit.app.Start`), port `8080`. |

Docker related files (Dockerfiles, build scripts and Docker Compose files to quickly start up hawkBit as monolith or as
micro services, with or without UI) live in the top level [docker](../docker) folder, see [docker/README.md](../docker/README.md).

Note: micro service setup requires all services using DB to use same shared DB. So, they don't work with default in
memory H2 database. Docker compose with mysql shows an example setup.
