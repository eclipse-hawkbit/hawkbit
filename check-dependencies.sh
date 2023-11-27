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

DASH_SUMMARY=".3rd-party/DEPENDENCIES"
DASH_REVIEW_SUMMARY=".3rd-party/DEPENDENCIES_REVIEW"

if [ -z "$1" ]
then
      DASH_IP_LAB=
else
      DASH_IP_LAB="-Ddash.review.summary=${DASH_REVIEW_SUMMARY} -Ddash.iplab.token=$1"
fi

mvn clean install -DskipTests -Ddash.skip=false \
  --projects '!org.eclipse.hawkbit:hawkbit-repository-test,!org.eclipse.hawkbit:hawkbit-dmf-rabbitmq-test' \
  -Ddash.summary=${DASH_SUMMARY} ${DASH_IP_LAB}