hawkBit Docker Build
===
This directory contains docker files for building hawkBit docker with two build types:

* _release_ - uses officially released hawkBit versions, downloading them from https://repo1.maven.org
* _development/dev_ - uses the local maven repository with built by developer (or just downloaded from any maven
  repository) hawkBit applications

Dockerfiles:

| File                    | Build type  | Application                                                                 |
|-------------------------|-------------|-----------------------------------------------------------------------------|
| `Dockerfile`            | release     | Spring Boot apps (update server, ddi/dmf/mgmt servers, ui, ...)             |
| `Dockerfile_dev`        | development | Spring Boot apps (update server, ddi/dmf/mgmt servers, ui, ...)             |
| `Dockerfile_dbinit`     | release     | `hawkbit-repository-jpa-init` - DB schema init/migration (Flyway), one shot |
| `Dockerfile_dbinit_dev` | development | `hawkbit-repository-jpa-init` - DB schema init/migration (Flyway), one shot |

## Build overview

Building images supports the following build arguments (i.e. ARG-s which could be passed using _--build-arg_):

* _JAVA_VERSION_ - **[OPTIONAL, if not set a default is used]** the Java version of the eclipse-temurin jre-alpine base
  image to be used.
* _HAWKBIT_APP_ - **[OPTIONAL, if not set _hawkbit-update-server_ is used (_hawkbit-repository-jpa-init_ for the dbinit
  Dockerfiles)]** the application to be built. Released images (see `.github/workflows/release_docker.yaml`) are:
  * micro-services: _hawkbit-ddi-server_, _hawkbit-dmf-server_, _hawkbit-mgmt-server_, _hawkbit-ui_
  * monolith: _hawkbit-update-server_
  * db init: _hawkbit-repository-jpa-init_ (built with _Dockerfile_dbinit_)
* _HAWKBIT_VERSION_ - **[MANDATORY for release builds, defaults to _0-SNAPSHOT_ for development builds]** the
  application version
* _CONTAINER_PORT_ - **[OPTIONAL, if not set 8080 is used]** on which the app opens the http server (if available)

Additionally, the _development_ builds shall be started with docker build context the local maven repository

## Build docker images

Docker image could be built, for example, with (`<version>` is an officially released hawkBit version available
on https://repo1.maven.org):

```shell
docker build --build-arg HAWKBIT_APP=hawkbit-update-server --build-arg HAWKBIT_VERSION=<version> -t hawkbit/hawkbit-update-server:<version> . -f Dockerfile
```

or just by:

```shell
docker build --build-arg HAWKBIT_VERSION=<version> -t hawkbit/hawkbit-update-server:<version> .
```

having that docker uses by default _Dockerfile_ and the _hawkbit-update-server_ is the default _HAWKBIT_APP_.

The DB init image is built with the dedicated Dockerfile:

```shell
docker build --build-arg HAWKBIT_VERSION=<version> -t hawkbit/hawkbit-repository-jpa-init:<version> . -f Dockerfile_dbinit
```

To build standard development docker images, e.g. snapshot based, you could use something like:

```shell
docker build -t hawkbit/hawkbit-update-server:0-SNAPSHOT -f Dockerfile_dev ~/.m2/repository
```

Note that here you have to use your maven repository containing the hawkBit app as docker build context, in the example
case _~/.m2/repository_ (so, run `mvn install` first).

To build all development images (ddi/dmf/mgmt servers, ui, update server, db init and mcp server) at once use
`build_dev.sh` (options: `-v <version, default 0-SNAPSHOT>`, `-r <maven repository, default ~/.m2/repository>`,
`-t <tag, default latest>`, optional application name to build only one):

```shell
./build_dev.sh
./build_dev.sh hawkbit-mgmt-server
```
