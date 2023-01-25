/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.json.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.springframework.hateoas.RepresentationModel;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "active", "initiator", "remark", "activatedAt" })
public class DdiAutoConfirmationState extends RepresentationModel<DdiAutoConfirmationState> {

    @NotNull
    private boolean active;
    private String initiator;
    private String remark;

    private Long activatedAt;

    /**
     * Constructor.
     */
    public DdiAutoConfirmationState() {
        // needed for json create.
    }

    public static DdiAutoConfirmationState active(final long activatedAt) {
        final DdiAutoConfirmationState state = new DdiAutoConfirmationState();
        state.setActive(true);
        state.setActivatedAt(activatedAt);
        return state;
    }

    public static DdiAutoConfirmationState disabled() {
        return new DdiAutoConfirmationState();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    public Long getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(final long activatedAt) {
        this.activatedAt = activatedAt;
    }

    public String getInitiator() {
        return initiator;
    }

    public void setInitiator(final String initiator) {
        this.initiator = initiator;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

}
