/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionset;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for invalidate DistributionSet requests.
 *
 */
public class MgmtInvalidateDistributionSetRequestBody {

    @NotNull
    @JsonProperty
    private MgmtCancelationType actionCancelationType;
    @JsonProperty
    private boolean cancelRollouts;

    public MgmtCancelationType getActionCancelationType() {
        return actionCancelationType;
    }

    public void setActionCancelationType(final MgmtCancelationType actionCancelationType) {
        this.actionCancelationType = actionCancelationType;
    }

    public boolean isCancelRollouts() {
        return cancelRollouts;
    }

    public void setCancelRollouts(final boolean cancelRollouts) {
        this.cancelRollouts = cancelRollouts;
    }

}
