#!/bin/bash
#
# Copyright (c) 2015 Bosch Software Innovations GmbH and others.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
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
