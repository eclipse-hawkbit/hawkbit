#!/bin/bash
#*******************************************************************************
# Copyright (c) 2021, 2022 Contributors to the Eclipse Foundation
#
# See the NOTICE file(s) distributed with this work for additional
# information regarding copyright ownership.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License 2.0 which is available at
# http://www.eclipse.org/legal/epl-2.0
#
# SPDX-License-Identifier: EPL-2.0
#*******************************************************************************

DASH_LICENSE_JAR=$1
shift

if [ ! -f "$DASH_LICENSE_JAR" ]; then
  echo "This script can be used to update the DEPENDENCIES"
  echo "file with the result of checking the Hawkbit maven"
  echo "dependencies using the Dash License Tool."
  echo ""
  echo "Usage: $0 <org.eclipse.dash.licenses jar path> [<other dash-tool parameters>..]"
  exit 1
fi

HAWKBIT_MAVEN_DEPS=".3rd-party/hawkbit-maven.deps"
DEPENDENCIES=".3rd-party/DEPENDENCIES"

# The spotbugs artifacts are being used with scope "provided", i.e. they are not copied to the
# container images and are not used/required during runtime and thus constitute something like
# a "works-with" dependency. It therefore seems ok to simply exclude them from the license check
# even though they are LGPL 2.1.
mvn dependency:list \
  -DexcludeGroupIds=org.eclipse,org.junit,com.github.spotbugs \
  -pl '!org.eclipse.hawkbit:hawkbit-repository-test,!org.eclipse.hawkbit:hawkbit-dmf-rabbitmq-test' | \
  grep -Poh "\S+:(runtime|compile|provided)" | \
  sed -e 's/^\(.*\)\:.*$/\1/' | \
  sort | \
  uniq > $HAWKBIT_MAVEN_DEPS

java -Dorg.eclipse.dash.timeout=60 -jar "${DASH_LICENSE_JAR}" -batch 90 -summary ${DEPENDENCIES} ${HAWKBIT_MAVEN_DEPS} "$@"
sort -o ${DEPENDENCIES} ${DEPENDENCIES}