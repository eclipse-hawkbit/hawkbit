#!/bin/bash
#*******************************************************************************
# Copyright (c) 2023 Bosch.IO GmbH and others.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
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

mvn dependency:list \
  -DexcludeGroupIds=org.eclipse,org.junit \
  -pl '!org.eclipse.hawkbit:hawkbit-repository-test,!org.eclipse.hawkbit:hawkbit-dmf-rabbitmq-test' | \
  grep -Poh "\S+:(runtime|compile|provided)" | \
  sed -e 's/^\(.*\)\:.*$/\1/' | \
  sort | \
  uniq > $HAWKBIT_MAVEN_DEPS

java -Dorg.eclipse.dash.timeout=60 -jar "${DASH_LICENSE_JAR}" -batch 90 -summary ${DEPENDENCIES} ${HAWKBIT_MAVEN_DEPS} "$@"
sort -o ${DEPENDENCIES} ${DEPENDENCIES}