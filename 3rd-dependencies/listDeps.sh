#!/bin/sh
#
# Copyright (c) 2015 Bosch Software Innovations GmbH and others.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
cd ..
mvn dependency:list -DexcludeGroupIds=org.eclipse -Dsort=true -DoutputFile=dependencies.txt
find . -name dependencies.txt|while read i; do cat $i;done|grep '.*:.*:compile'|sort|uniq > 3rd-dependencies/compile.txt
find . -name dependencies.txt|while read i; do cat $i;done|grep '.*:.*:test'|sort|uniq > 3rd-dependencies/test.txt
find . -name dependencies.txt|while read i; do cat $i;done|grep '.*:.*:provided'|sort|uniq > 3rd-dependencies/provided.txt
find . -name dependencies.txt|while read i; do rm $i;done
cd 3rd-dependencies/
cat compile.txt provided.txt|cut -d':' -f1-4|while read i; do grep -h $i test.txt;done|sort|uniq|while read x; do sed -i.bak -e s/$x// test.txt ;done
sed -i.bak '/^[[:space:]]*$/d' test.txt
rm *.bak
