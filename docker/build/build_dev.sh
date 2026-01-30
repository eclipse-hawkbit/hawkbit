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
# -r <local maven repository> the local maven repository the already built application jars are located into
# -t <docker tag> the docker tag the build image(s) will be tagged with
# <application> - if not passed all images will be built

VERSION=0-SNAPSHOT
MVN_REPO=~/.m2/repository
TAG=latest
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
cd "${SCRIPT_DIR}"

while getopts v:r:t: option
do
    case "${option}"
        in
        v)VERSION=${OPTARG};;
        r)MVN_REPO=${OPTARG};;
        t)TAG=${OPTARG};;
        *) echo "Usage: $0 -v <version default 0-SNAPSHOT> -r <repository default ~/.m2/repository> -t <tag default latest> <application default all>"; exit 1;;
    esac
done
# Shift arguments after pots
shift $((OPTIND - 1))

echo "hawkBit version  : ${VERSION}"
echo "maven repository : ${MVN_REPO}"
echo "docker tag       : ${TAG}"

function build() {
  if [ "$1" == "hawkbit-repository-jpa-init" ]; then
      DOCKER_FILE="Dockerfile_dbinit_dev"
  else
      DOCKER_FILE="Dockerfile_dev"
  fi
  echo "docker file          : ${DOCKER_FILE}"

  docker buildx build -t hawkbit/$1:${TAG} --build-arg HAWKBIT_APP=$1 --build-arg HAWKBIT_VERSION=${VERSION} -f ${DOCKER_FILE} "${MVN_REPO}"
}

if [ -z "$1" ]; then
    echo "Build all"
    # micro-services
    build "hawkbit-ddi-server"
    build "hawkbit-dmf-server"
    build "hawkbit-mgmt-server"
    build "hawkbit-ui"
    # monolith
    build "hawkbit-update-server"
    # db init
    build "hawkbit-repository-jpa-init"
    # mcp server
    build "hawkbit-mcp-server"
else
    echo "Build $1"
    build $1
fi
