#
# Copyright (c) 2018 Bosch Software Innovations GmbH and others
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#

#!/bin/bash

CURRENT_DIR=$(pwd)

# Checking for Redoc CLI and npm
npx @redocly/cli --version > /dev/null 2>&1

if [ $? != 0 ]; then
    echo "[ERROR] Redoc CLI is not installed! Please make suer to install it before trying again."
    exit 1
fi

# Execute the npx command
npx @redocly/cli build-docs ${CURRENT_DIR}/content/rest-api/mgmt.yaml -o ${CURRENT_DIR}/content/rest-api/mgmt.html

if [ $? != 0 ]; then
    echo "[ERROR] Failed to execute the Redoc CLI command form MGMT API."
    exit 1
else
    echo "[INFO] Successfully executed the Redoc CLI command for MGMT API."
fi

# Execute the npx command
npx @redocly/cli build-docs ${CURRENT_DIR}/content/rest-api/ddi.yaml -o ${CURRENT_DIR}/content/rest-api/ddi.html

if [ $? != 0 ]; then
    echo "[ERROR] Failed to execute the Redoc CLI command form DDI API."
    exit 1
else
    echo "[INFO] Successfully executed the Redoc CLI command for DDI API."
fi