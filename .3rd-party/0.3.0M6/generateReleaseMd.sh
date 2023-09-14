#!/bin/bash
#
# Copyright (c) 2015 Bosch Software Innovations GmbH and others
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
echo "# 3rd party dependencies for Release X.X.X"
echo ""
echo "## Eclipse CQs - Provided/compile"
echo ""
echo "| Group ID  | Artifact ID  | Version  | CQ  |"
echo "|---|---|---|---|---|"
cat compile.txt provided.txt|cut -d':' -f1,2,4|sed -e 's/:/|/g'|while read i; do echo "|$i| []() |";done
echo ""
echo "## Test and build dependencies"
echo ""
echo "CQ: "
echo ""
echo "| Group ID  | Artifact ID  | Version  |"
echo "|---|---|---|"
cut -d':' -f1,2,4 test.txt|sed -e 's/:/|/g'|while read i; do echo "|$i|";done
