hawkBit Runtime
===


| Folder                   | Description                                                                                                                |
|--------------------------|----------------------------------------------------------------------------------------------------------------------------|
| `.sandbox/`              | Content of the hawkBit sandbox installation running on [hawkbit.eclipse.org](https://hawkbit.eclipse.org/UI/).             |
| `docker/`                | Docker related files, such es Dockerfiles, compose and stack files to quickly start up an hawkBit.                         |
| `docker/docker_build/`   | Docker images build related files, such es Dockerfiles and build shell scripts.                                            |
| `hawkbit-update-server/` | Spring-Boot application of hawkBit. Monolith containing all services.                                                      |
| `hawkbit-ddi-server/`    | Spring-Boot application of hawkBit DDI server.                                                                             |
| `hawkbit-dmf-server/`    | Spring-Boot application of hawkBit DMF server.                                                                             |
| `hawkbit-mgmt-server/`   | Spring-Boot application of hawkBit Management server. Provides REST Management API and rollouts / auto assigment processing |

Note: micro service setup requires all services using DB to use same shared DB. So, they don't work with default in memory H2 database. Docker compose with mysql shows an example setup.
