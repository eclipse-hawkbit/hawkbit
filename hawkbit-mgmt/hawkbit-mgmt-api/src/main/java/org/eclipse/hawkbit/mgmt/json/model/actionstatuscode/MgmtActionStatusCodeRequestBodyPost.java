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

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.eclipse.hawkbit.mgmt.json.model.MgmtNamedEntity;

/**
 * Request body for creating an action status code via Management API.
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(example = """
        {
          "code" : 1234,
          "name" : "DOWNLOAD_HASH_MISMATCH",
          "description" : "Hash mismatch during download",
          "severity" : "ERROR"
        }""")
public class MgmtActionStatusCodeRequestBodyPost extends MgmtNamedEntity {

    @NotNull
    @JsonProperty(required = true)
    @Schema(description = "The numeric status code", example = "1234")
    private Integer code;

    @Schema(description = "The severity level (defaults to INFO if not specified)", example = "ERROR")
    private MgmtActionStatusCodeSeverity severity;
}
