hawkBit Runtime
===

| Folder                 | Description                                                                                                                  |
|------------------------|------------------------------------------------------------------------------------------------------------------------------|
| `.sandbox/`            | Content of the hawkBit sandbox installation running on [hawkbit.eclipseprojects.io](https://hawkbit.eclipseprojects.io/UI/). |
| `docker/`              | Docker related files, such es Dockerfiles, compose and stack files to quickly start up an hawkBit.                           |
| `docker/docker_build/` | Docker images build related files, such es Dockerfiles and build shell scripts.                                              |

Note: micro service setup requires all services using DB to use same shared DB. So, they don't work with default in
memory H2 database. Docker compose with mysql shows an example setup.
