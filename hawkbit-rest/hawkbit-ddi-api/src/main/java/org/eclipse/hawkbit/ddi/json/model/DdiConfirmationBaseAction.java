/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.json.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.springframework.hateoas.RepresentationModel;

import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Update action resource.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "id", "confirmation", "actionHistory" })
public class DdiConfirmationBaseAction extends RepresentationModel<DdiConfirmationBaseAction> {

    @JsonProperty("id")
    @NotNull
    private String id;

    @JsonProperty("confirmation")
    @NotNull
    private DdiDeployment confirmation;

    /**
     * Action history containing current action status and a list of feedback
     * messages received earlier from the controller.
     */
    @JsonProperty("actionHistory")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DdiActionHistory actionHistory;

    /**
     * Constructor.
     */
    public DdiConfirmationBaseAction() {
        // needed for json create.
    }

    /**
     * Constructor.
     *
     * @param id
     *            of the update action
     * @param confirmation
     *            chunk details
     * @param actionHistory
     *            containing current action status and a list of feedback messages
     *            received earlier from the controller.
     */
    public DdiConfirmationBaseAction(final String id, final DdiDeployment confirmation,
                                     final DdiActionHistory actionHistory) {
        this.id = id;
        this.confirmation = confirmation;
        this.actionHistory = actionHistory;
    }

    /**
     * Return the confirmation data containing chunks needed to confirm a action.
     * 
     * @return {@link DdiDeployment}
     */
    public DdiDeployment getConfirmation() {
        return confirmation;
    }

    /**
     * Returns the action history containing current action status and a list of
     * feedback messages received earlier from the controller.
     *
     * @return {@link DdiActionHistory}
     */
    public DdiActionHistory getActionHistory() {
        return actionHistory;
    }

    @Override
    public String toString() {
        return "ConfirmationBase [id=" + id + ", confirmation=" + confirmation + " actionHistory=" + actionHistory
                + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final DdiConfirmationBaseAction oBase = (DdiConfirmationBaseAction) obj;
        return Objects.equals(this.id, oBase.id) && Objects.equals(this.confirmation, oBase.confirmation)
                && Objects.equals(this.actionHistory, oBase.actionHistory);
    }

}
