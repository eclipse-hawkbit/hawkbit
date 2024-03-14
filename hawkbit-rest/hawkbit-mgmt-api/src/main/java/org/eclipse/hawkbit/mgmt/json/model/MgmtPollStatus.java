/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import static org.checkerframework.checker.units.qual.Prefix.exa;

/**
 * A json annotated rest model for PollStatus to RESTful API representation.
 */
@Data
@Accessors(chain = true)
@ToString
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtPollStatus {

    @JsonProperty
    @Schema(example = "1691065941102")
    private Long lastRequestAt;

    @JsonProperty
    @Schema(example = "1691109141102")
    private Long nextExpectedRequestAt;

    @JsonProperty
    @Schema(example = "false")
    private boolean overdue;
}