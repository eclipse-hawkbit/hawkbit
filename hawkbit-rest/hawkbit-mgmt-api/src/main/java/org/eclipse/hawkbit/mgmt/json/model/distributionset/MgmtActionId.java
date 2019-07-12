/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionset;

import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MgmtActionId extends ResourceSupport {

    private long actionId;

    public MgmtActionId() {
    }

    /**
     * Constructor
     * @param actionId
     *              the actionId
     */
    public MgmtActionId(final long actionId) {
        this.actionId = actionId;
    }

    @JsonProperty("id")
    public long getActionId() {
        return actionId;
    }

    public void setActionId(final long actionId) {
        this.actionId = actionId;
    }
}
