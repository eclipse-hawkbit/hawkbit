#
# Copyright (c) 2015 Bosch Software Innovations GmbH and others.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#

# This script allows the deployment of the complete hawkBit sandbox including
# data example to a cloud foundry enviroment. Expects existing CF CLI
# installation and login to be existing already.

cf stop hawkbit-simulator
cd examples/hawkbit-example-app/target/
cf push
cd ../..
java -jar hawkbit-example-mgmt-simulator/target/hawkbit-example-mgmt-simulator-0.2.0-SNAPSHOT.jar --hawkbit.url=https://hawkbit.eu-gb.mybluemix.net
cd hawkbit-device-simulator/target/
cf push
cd ../../..
