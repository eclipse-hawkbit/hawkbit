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

# Provided and compile (excludes the test modules)
mvn dependency:list -B -DexcludeGroupIds=org.eclipse,org.aspectj -pl '!org.eclipse.hawkbit:hawkbit-repository-test,!org.eclipse.hawkbit:hawkbit-dmf-rabbitmq-test'  -Dsort=true -DoutputFile=dependencies.txt
find . -name dependencies.txt|while read i; do cat $i;done|grep '.*:.*:compile'| tr -d '[:blank:]'| sed -e 's/(optional)//'|sort|uniq > .3rd-party/compile.txt
find . -name dependencies.txt|while read i; do cat $i;done|grep '.*:.*:provided'| tr -d '[:blank:]'| sed -e 's/(optional)//'|sort|uniq > .3rd-party/provided.txt

# Test dependencies
mvn dependency:list -B -DexcludeGroupIds=org.eclipse -Dsort=true -DoutputFile=dependencies.txt
find . -name dependencies.txt|while read i; do cat $i;done|grep '.*:.*:test'| tr -d '[:blank:]'| sed -e 's/(optional)//'|sort|uniq > .3rd-party/test.txt

# Cleanup temp files
find . -name dependencies.txt|while read i; do rm $i;done

# Sort and order content
cd .3rd-party/
cat compile.txt provided.txt|cut -d':' -f1-4|while read i; do grep -h $i test.txt;done|sort|uniq|while read x; do sed -i.bak -e s/$x// test.txt ;done
sed -i.bak '/^[[:space:]]*$/d' test.txt
rm *.bak
