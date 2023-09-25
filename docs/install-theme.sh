#
# Copyright (c) 2018 Bosch Software Innovations GmbH and others
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#

# This script checks if 'hugo' is installed. Afterwards, the Hugo theme is downloaded.

#!/bin/bash


hugo version
if [ $? != 0 ]
then
    echo "[ERROR] Please install Hugo first before proceeding."
    exit 1
fi

echo "[INFO] "
echo "[INFO] Install Hugo Theme"
HUGO_THEMES=themes/hugo-material-docs

if [ ! -d ${HUGO_THEMES} ]
then
    git submodule add --force https://github.com/digitalcraftsman/hugo-material-docs.git ${HUGO_THEMES}
    echo "[INFO] ... done"
else
    echo "[INFO] ... theme already installed in: ${HUGO_THEMES}"
fi

echo "[INFO] "
echo "[INFO] Launch the documentation locally by running 'mvn site' (or 'hugo server' in the docs directory),"
echo "[INFO] and browse to 'http://localhost:1313/hawkbit/'. "