/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.action;

import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated model for Action updates in RESTful API representation.
 *
 */
public class MgmtActionRequestBodyPut {

    @JsonProperty
    private MgmtActionType forceType;

    public MgmtActionType getForceType() {
        return forceType;
    }

    public void setForceType(final MgmtActionType forceType) {
        this.forceType = forceType;
    }

}
