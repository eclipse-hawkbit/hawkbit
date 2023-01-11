/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.target;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload to activate the auto-confirmation by given initiator and remark.
 */
public class MgmtTargetAutoConfirmUpdate {
    @JsonProperty(required = false)
    private final String initiator;

    @JsonProperty(required = false)
    private final String remark;

    /**
     * Constructor.
     *
     * @param initiator
     *            can be null
     * @param remark
     *            can be null
     */
    @JsonCreator
    public MgmtTargetAutoConfirmUpdate(@JsonProperty(value = "initiator") final String initiator,
            @JsonProperty(value = "remark") final String remark) {
        this.initiator = initiator;
        this.remark = remark;
    }

    public String getInitiator() {
        return initiator;
    }

    public String getRemark() {
        return remark;
    }

    @Override
    public String toString() {
        return "MgmtTargetAutoConfirm [initiator=" + initiator + ", remark=" + remark + ", toString()="
                + super.toString() + "]";
    }
}
