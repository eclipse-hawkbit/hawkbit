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
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.eclipse.hawkbit.mgmt.json.model.MgmtNamedEntity;

/**
 * Response body for action status code in the Management API.
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Action status code catalogue entry", example = """
        {
          "id" : 1,
          "code" : 1234,
          "name" : "DOWNLOAD_HASH_MISMATCH",
          "description" : "Hash mismatch during download",
          "severity" : "ERROR",
          "createdBy" : "admin",
          "createdAt" : 1682408568812,
          "lastModifiedBy" : "admin",
          "lastModifiedAt" : 1682408568812,
          "_links" : {
            "self" : {
              "href" : "https://management-api.host.com/rest/v1/action-status-codes/1"
            }
          }
        }""")
public class MgmtActionStatusCode extends MgmtNamedEntity {

    @JsonProperty(required = true)
    @Schema(description = "The technical ID of the action status code", example = "1")
    private Long id;

    @JsonProperty(required = true)
    @Schema(description = "The numeric status code", example = "1234")
    private int code;

    @JsonProperty(required = true)
    @Schema(description = "The severity level", example = "ERROR")
    private MgmtActionStatusCodeSeverity severity;
}
