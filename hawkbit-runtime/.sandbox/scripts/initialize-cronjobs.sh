#
# Copyright (c) 2018 Bosch Software Innovations GmbH and others.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#

SCRIPT_DIR="/.sandbox/scripts"
LOGFILE_DIR="/.sandbox/logs"

#                    m  h dom mon dow user  command
SANDBOX_CLEANUP_JOB="0  0    * * 7   root   ${SCRIPT_DIR}/sandbox-cleanup.sh >> ${LOGFILE_DIR}/\$(date +\%F)-hawkbit 2>&1"
LOGFILE_CLEANUP_JOB="0  0    1 * *   root   find ${LOGFILE_DIR}* -mtime +182 -exec rm {} \; >/dev/null 2>&1"

CRONTAB_FILE="/etc/crontab"


# Create directory for log files
mkdir -p ${LOGFILE_DIR}


echo "# Reset hawkBit stack once a week to delete all data" >> "${CRONTAB_FILE}"
echo "${SANDBOX_CLEANUP_JOB}" >> "${CRONTAB_FILE}"

echo "# Remove log files documenting reset, that are older than 6 months" >> "${CRONTAB_FILE}"
echo "${LOGFILE_CLEANUP_JOB}" >> "${CRONTAB_FILE}"