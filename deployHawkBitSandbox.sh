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

cf api https://api.eu-gb.bluemix.net
cf login
cf stop hawkbit-simulator
cd hawkbit-runtime/hawkbit-update-server/target/
cf push
cd ../../../examples/
java -jar hawkbit-example-mgmt-simulator/target/hawkbit-example-mgmt-simulator-0.2.0-SNAPSHOT-exec.jar --hawkbit.url=https://hawkbit.eu-gb.mybluemix.net
cd hawkbit-device-simulator/target/
cf push
cd ../../..
