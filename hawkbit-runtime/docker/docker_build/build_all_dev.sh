#!/bin/bash
#
# Copyright (c) 2023 Bosch.IO GmbH and others
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
set -xe

VERSION=0.4.0-SNAPSHOT
FLAVOUR="standard"
MVN_REPO=~/.m2/repository

while getopts v:f:r: option
do
    case "${option}"
        in
        v)VERSION=${OPTARG};;
        f)FLAVOUR=${OPTARG};;
        r)MVN_REPO=${OPTARG};;
    esac
done

echo "hawkBit version      : ${VERSION}"
echo "docker image flavour : ${FLAVOUR}"
echo "maven repository     : ${MVN_REPO}"

if [ ${FLAVOUR} == "mysql" ]
then
      DOCKER_FILE="Dockerfile_dev-mysql"
      TAG_SUFFIX="-mysql"
else
      DOCKER_FILE="Dockerfile_dev"
      TAG_SUFFIX=""
fi

echo "docker file          : ${DOCKER_FILE}"

docker build -t hawkbit/hawkbit-ddi-server:${VERSION}${TAG_SUFFIX} -t hawkbit/hawkbit-ddi-server:latest${TAG_SUFFIX} --build-arg HAWKBIT_APP=hawkbit-ddi-server --build-arg HAWKBIT_VERSION=${VERSION} -f ${DOCKER_FILE} "${MVN_REPO}"
docker build -t hawkbit/hawkbit-dmf-server:${VERSION}${TAG_SUFFIX} -t hawkbit/hawkbit-dmf-server:latest${TAG_SUFFIX} --build-arg HAWKBIT_APP=hawkbit-dmf-server --build-arg HAWKBIT_VERSION=${VERSION} -f ${DOCKER_FILE} "${MVN_REPO}"
docker build -t hawkbit/hawkbit-mgmt-server:${VERSION}${TAG_SUFFIX} -t hawkbit/hawkbit-mgmt-server:latest${TAG_SUFFIX} --build-arg HAWKBIT_APP=hawkbit-mgmt-server --build-arg HAWKBIT_VERSION=${VERSION} -f ${DOCKER_FILE} "${MVN_REPO}"
docker build -t hawkbit/hawkbit-vv8-ui:${VERSION}${TAG_SUFFIX} -t hawkbit/hawkbit-vv8-ui:latest${TAG_SUFFIX} --build-arg HAWKBIT_APP=hawkbit-vv8-ui --build-arg HAWKBIT_VERSION=${VERSION} -f ${DOCKER_FILE} "${MVN_REPO}"
