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
        . or ($item.tags? | index("DDI Root Controller")) == null
      )
    )
  )
  | .tags |= map(select(.name | contains("DDI") | not))
  | .components.schemas = (.components.schemas | with_entries(select(.key | startswith("Ddi") | not)))
' "$input_file" > "$mgmt_file"

jq '
  .paths |= with_entries(
    select(
      reduce .value[] as $item (
        false;
        . or ($item.tags? | index("DDI Root Controller")) != null
      )
    )
  )
  | .tags |= map(select(.name | contains("DDI")))
  | .components.schemas = (
        .components.schemas
        | with_entries(
            select(
                (.key | startswith("Ddi"))
                or (.key | . == "Link")
                or (.key | . == "ExceptionInfo")
            )
        )
    )
' "$input_file" > "$ddi_file"

