/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.json.model;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.hateoas.RepresentationModel;

/**
 * Confirmation base response.
 * Set order to place links at last.
 */
@NoArgsConstructor // needed for json create
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "autoConfirm" })
@Schema(
        description = """
                **_links**:
                * **confirmationBase** - confirmation base
                * **deactivateAutoConfirm** - where to deactivate auto confirm
                * **activateAutoConfirm** - where to activate auto confirm
                """,
        example = """
                {
                  "autoConfirm" : {
                    "active" : false
                  },
                  "_links" : {
                    "activateAutoConfirm" : {
                      "href" : "https://management-api.host.com/TENANT_ID/controller/v1/CONTROLLER_ID/confirmationBase/activateAutoConfirm"
                    },
                    "confirmationBase" : {
                      "href" : "https://management-api.host.com/TENANT_ID/controller/v1/CONTROLLER_ID/confirmationBase/10?c=-2122565939"
                    }
                  }
                }""")
public class DdiConfirmationBase extends RepresentationModel<DdiConfirmationBase> {

    @JsonProperty("autoConfirm")
    @NotNull
    private DdiAutoConfirmationState autoConfirm;

    public DdiConfirmationBase(final DdiAutoConfirmationState autoConfirmState) {
        this.autoConfirm = autoConfirmState;
    }
}