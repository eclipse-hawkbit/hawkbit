/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.target;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.hateoas.RepresentationModel;

/**
 * Response representing the current state of auto-confirmation for a specific target
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "active", "initiator", "remark", "activatedAt" })
@Schema(description = """
        **_links**:
        * **deactivate** - Reference link to deactivate auto confirm (present if active)
        """, example = """
        {
          "active" : true,
          "initiator" : "custom_initiator_value",
          "remark" : "custom_remark",
          "activatedAt" : 1682408577704,
          "_links" : {
            "deactivate" : {
              "href" : "https://management-api.host.com/rest/v1/targets/137/autoConfirm/deactivate"
            }
          }
        }""")
public class MgmtTargetAutoConfirm extends RepresentationModel<MgmtTargetAutoConfirm> {

    /**
     * The target URL mapping, href link activate auto-confirm on a target.
     */
    public static final String ACTIVATE = "activate";
    /**
     * The target URL mapping, href link deactivate auto-confirm on a target.
     */
    public static final String DEACTIVATE = "deactivate";
    @NotNull
    @Schema(description = "Flag if auto confirm is active", example = "true")
    private boolean active;

    @Schema(description = "Initiator set on activation", example = "custom_initiator_value")
    private String initiator;

    @Schema(description = "Remark set on activation", example = "custom_remark")
    private String remark;

    @Schema(description = "Timestamp of the activation", example = "1691065938576")
    private Long activatedAt;

    public static MgmtTargetAutoConfirm active(final long activatedAt) {
        final MgmtTargetAutoConfirm state = new MgmtTargetAutoConfirm();
        state.setActive(true);
        state.setActivatedAt(activatedAt);
        return state;
    }

    public static MgmtTargetAutoConfirm disabled() {
        return new MgmtTargetAutoConfirm();
    }
}