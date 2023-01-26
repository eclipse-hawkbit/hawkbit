/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.json.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiActivateAutoConfirmation {

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
    public DdiActivateAutoConfirmation(@JsonProperty(value = "initiator") final String initiator,
            @JsonProperty(value = "remark") final String remark) {
        this.initiator = initiator;
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "DdiActivateAutoConfirmation [initiator=" + initiator + ", remark=" + remark + ", toString()="
                + super.toString() + "]";
    }

    public String getInitiator() {
        return initiator;
    }

    public String getRemark() {
        return remark;
    }
}
