#!/bin/sh
#
# Copyright (c) 2015 Bosch Software Innovations GmbH and others
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#

#echo "$CIRCLE_PULL_REQUEST pull request"

# Run SonarQube only for master branch
if [ $CIRCLE_BRANCH = master ] ; then
  mvn verify license:check javadoc:javadoc org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.branch.name=eclipse-master -Dsonar.login=$SONAR_ACCESS_TOKEN --batch-mode
else
  mvn verify license:check javadoc:javadoc --batch-mode
fi
