/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.action;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * A json annotated rest model for ActionStatus to RESTful API representation.
 */
@Data
@Accessors(chain = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtActionStatus {

    @JsonProperty("id")
    @Schema(example = "21")
    private Long statusId;

    @JsonProperty
    @Schema(example = "running")
    private String type;

    @JsonProperty
    private List<String> messages;

    @JsonProperty
    @Schema(example = "1691065929524")
    private Long reportedAt;

    @JsonProperty
    @Schema(example = "200")
    private Integer code;
}