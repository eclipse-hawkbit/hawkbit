#
# Copyright (c) 2018 Bosch Software Innovations GmbH and others
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#

# This script is used to clean up the previously generated or downloaded files.

#!/bin/bash


echo "[INFO] Remove Hugo Theme"
rm -rf themes resources public
echo "[INFO] ... done"

echo "[INFO] "

echo "[INFO] Remove generated REST docs"
rm -f content/rest-api/*.json
rm -f content/rest-api/*.html
echo "[INFO] ... done"


