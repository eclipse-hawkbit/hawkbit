#!/bin/sh
#
# Copyright (c) 2015 Bosch Software Innovations GmbH and others.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#

echo $CI_PULL_REQUEST pull request
# regular sonar on master
if [ "$CIRCLE_BRANCH" = "master" ]; then
  mvn verify license:check sonar:sonar -Dsonar.login=$SONAR_SERVER_TOKEN
# preview in case of pull request - disabled as circle does not fill those with pull reuqests from different directories
else
  #if [ -n "$CI_PULL_REQUEST" ]; then
  #  mvn org.jacoco:jacoco-maven-plugin:prepare-agent verify license:check sonar:sonar -B -e -V \
  #    -Dclirr=true \
  #    -Dsonar.analysis.mode=issues \
  #    -Dsonar.github.pullRequest=`echo $CI_PULL_REQUEST| awk -F'/' '{print $7}'` \
  #    -Dsonar.github.login=$SONAR_GITHUB_LOGIN \
  #    -Dsonar.github.oauth=$SONAR_GITHUB_OAUTH \
  #    -Dsonar.login=$SONAR_SERVER_USER \
  #    -Dsonar.password=$SONAR_SERVER_PASSWD
  #else
    mvn verify license:check
  #fi
fi
# but noting in case of other branches
