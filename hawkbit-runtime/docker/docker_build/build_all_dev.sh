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

# Usage: builds all docker images. Use:
# -v <version> to pass version
# -f <flavour> to pass flavour. "mysql" stands for MySQL while all the rest (and default) is assumed Standard
# -r <local maven repository> the local maven repository the already built application jars are located into

VERSION=0.4.1
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

function build() {
  docker build -t hawkbit/$1:${VERSION}${TAG_SUFFIX} -t hawkbit/$1:latest${TAG_SUFFIX} --build-arg HAWKBIT_APP=$1 --build-arg HAWKBIT_VERSION=${VERSION} -f ${DOCKER_FILE} "${MVN_REPO}"
}

build "hawkbit-ddi-server"
build "hawkbit-dmf-server"
build "hawkbit-mgmt-server"
build "hawkbit-vv8-ui"
build "hawkbit-simple-ui"

build "hawkbit-update-server"
