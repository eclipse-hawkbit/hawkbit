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
input_file=${CURRENT_DIR}/content/rest-api/openapi.json
mgmt_file=${CURRENT_DIR}/content/rest-api/mgmt.json
ddi_file=${CURRENT_DIR}/content/rest-api/ddi.json

jq '
  .paths |= with_entries(
    select(
      reduce .value[] as $item (
        false;
        . or ($item.tags? | index("ddi-root-controller")) == null
      )
    )
  )
  | .tags |= map(select(.name | contains("DDI") | not))
' "$input_file" > "$mgmt_file"

jq '
  .paths |= with_entries(
    select(
      reduce .value[] as $item (
        false;
        . or ($item.tags? | index("ddi-root-controller")) != null
      )
    )
  )
  | .tags |= map(select(.name | contains("DDI")))
' "$input_file" > "$ddi_file"

