#
# Copyright (c) 2018 Bosch Software Innovations GmbH and others.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#

# This script is used to clean up the previously generated or downloaded files.

#!/bin/bash


echo "[INFO] Remove Hugo Theme"
rm -rf themes resources public
echo "[INFO] ... done"

echo "[INFO] "

echo "[INFO] Remove generated REST docs"
rm -f content/rest-api/*.html
echo "[INFO] ... done"


