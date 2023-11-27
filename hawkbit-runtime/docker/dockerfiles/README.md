hawkBit Docker
===
Build files for both hawkbit docker flavours - standard (no mysql driver) and mysql (with MariaDB Java connector with support for MySQL).

## Build overview
Images are built using _Dockerfile_ or _Dockerfile-mysql_. They accept _ARG_ build parameters _JAVA_VERSION_, _HAWKBIT_APP_ (by default hawkbit-update-server), HAWKBIT_VERSION_ (by default the last released hawkBit version), _CONTAINER_PORT_ (by default 8080) on which the app opens the http server (if available) and (for mysql flavour only) _MARIADB_DRIVER_VERSION_ (by default 3.1.4 at the time of writing).
Both flavours of are almost the same, just mysql has in addition a MariaDB Java connector and is started, by default, with mysql Spring profile.

## Build standard
Standard flavour could be build, for example, with:
```shell
docker build --build-arg HAWKBIT_APP=hawkbit-update-server -t hawkbit_update_server:0.3.0 . -f Dockerfile
```
or just by:
```shell
docker build --build-arg HAWKBIT_APP=hawkbit-update-server -t hawkbit_update_server:0.3.0 .
```
having that docker uses by default _Dockerfile_.

## Build mysql
Mysql flavour could be build, for example, with:
```shell
docker build --build-arg HAWKBIT_APP=hawkbit-update-server -t hawkbit_update_server:0.3.0-mysql . -f Dockerfile-mysql
```