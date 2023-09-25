/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
