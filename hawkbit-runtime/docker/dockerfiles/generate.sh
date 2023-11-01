#!/bin/bash
#
# Copyright (c) 2018 Bosch Software Innovations GmbH and others
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#

set -euxo pipefail

HAWKBIT_VERSION="0.3.0M9"
MARIADB_DRIVER_VERSION="3.1.4"
BASE_IMAGE="eclipse-temurin:17.0.9_9-jre-alpine"

##################################################

#
# rm ./Dockerfile ./Dockerfile-mysql
cp ./Dockerfile.template ./Dockerfile
cp ./Dockerfile-mysql.template ./Dockerfile-mysql

sed -i '' -e "s/{{BASE_IMAGE}}/${BASE_IMAGE}/g; s/{{HAWKBIT_VERSION}}/$HAWKBIT_VERSION/g" ./Dockerfile
sed -i '' -e "s/{{HAWKBIT_VERSION}}/$HAWKBIT_VERSION/g; s/{{MARIADB_DRIVER_VERSION}}/$MARIADB_DRIVER_VERSION/g" ./Dockerfile-mysql
