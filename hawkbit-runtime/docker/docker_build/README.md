hawkBit Docker Build
===
This directory contains docker files for building both hawkBit docker flavours:
* _standard_ - hawkBit images without mysql driver
* _mysql_ - with MariaDB Java connector with support for MySQL.
  
Both flavours of are almost the same, just mysql has in addition a MariaDB Java connector and is started, by default, with mysql Spring profile.

For every flavour there are two build types:
* _release_ - uses officially released hawkBit versions, downloading them from https://repo1.maven.org
* _development/dev_ - uses the local maven repository with built by developer (or just downloaded from any maven repository) hawkBit applications 

## Build overview
Building images supports the following build arguments (i.e. ARG-s which could be passed using _--build-arg_):
* _JAVA_VERSION_ - **[OPTIONAL, if not set a default is used]** the Java version of the eclipse-temurin jre-alpine base image to be used.   
* _HAWKBIT_APP_ - **[OPTIONAL, if not set _hawkbit-update-server_ is used]** the application to be build. Currently, there is just _hawkbit-update-server_ but in future, if hawkBit is split to micro-services, there could be different micro-service apps.
* _HAWKBIT_VERSION_ - **[OPTIONAL, if not set a default, should be the last officially released version, is used]** the application version
* _CONTAINER_PORT_ - **[OPTIONAL, if not set 8080 is used]** on which the app opens the http server (if available)
* _MARIADB_DRIVER_VERSION_ (mysql flavours only!) - **[OPTIONAL, if not set a default is used]** the version of MariaDB connector to be used

Additionally, tge _development_ builds shall be started with docker build context the local maven repository

## Build standard
Standard flavour could be build, for example, with (fixed version 0.4.1 is just an example):
```shell
docker build --build-arg HAWKBIT_APP=hawkbit-update-server --build-arg HAWKBIT_VERSION=0.4.1 -t hawkbit_update_server:0.4.1 . -f Dockerfile
```
or just by:
```shell
docker build --build-arg HAWKBIT_VERSION=0.4.1 -t hawkbit_update_server:0.4.1 .
```
having that docker uses by default _Dockerfile_ and the _hawkbit-update-server_ is the default _HAWKBIT_APP_.

To build standard development docker images, e.g. snapshot based, you could use something like:
```shell
docker build -t hawkbit_update_server:0-SNAPSHOT -f Dockerfile_dev ~/.m2/repository
```
Note that here you have to use your maven repository containing the hawkBit app as docker build context, in the example case _~/.m2/repository_ 

## Build mysql
Mysql flavour could be build, for example, with:
```shell
docker build --build-arg HAWKBIT_APP=hawkbit-update-server --build-arg HAWKBIT_VERSION=0.4.1 -t hawkbit_update_server:0.4.1-mysql . -f Dockerfile-mysql
```
or just by:
```shell
docker build --build-arg -t hawkbit_update_server:0.4.1-mysql --build-arg HAWKBIT_VERSION=0.4.1 . -f Dockerfile-mysql
```
having that the _hawkbit-update-server_ is the default _HAWKBIT_APP_.

To build development mysql docker images, e.g. snapshot based, you could use something like:
```shell
docker build -t hawkbit_update_server:0-SNAPSHOT-mysql -f Dockerfile_dev-mysql ~/.m2/repository
```
Note that here you have to use your maven repository containing the hawkBit app as docker build context, in the example case _~/.m2/repository_