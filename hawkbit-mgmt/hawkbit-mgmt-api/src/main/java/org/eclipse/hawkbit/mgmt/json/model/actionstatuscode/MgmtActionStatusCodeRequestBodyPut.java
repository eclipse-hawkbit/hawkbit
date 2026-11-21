/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.actionstatuscode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Request body for updating an action status code via Management API.
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(example = """
        {
          "name" : "DOWNLOAD_HASH_MISMATCH",
          "description" : "Hash mismatch during download",
          "severity" : "WARN"
        }""")
public class MgmtActionStatusCodeRequestBodyPut {

    @Schema(description = "The name", example = "DOWNLOAD_HASH_MISMATCH")
    private String name;

    @Schema(description = "The description", example = "Hash mismatch during download")
    private String description;

    @Schema(description = "The severity level", example = "WARN")
    private MgmtActionStatusCodeSeverity severity;
}
