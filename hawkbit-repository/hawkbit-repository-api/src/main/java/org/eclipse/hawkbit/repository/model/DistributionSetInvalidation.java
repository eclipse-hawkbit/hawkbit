/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

public class DistributionSetInvalidation {

    private long setId;
    private CancelationType cancelationType;
    private boolean cancelRollouts;
    private boolean cancelAutoAssignments;

    public DistributionSetInvalidation(final long setId, final CancelationType cancelationType,
            final boolean cancelRollouts, final boolean cancelAutoAssignments) {
        this.setId = setId;
        this.cancelationType = cancelationType;
        this.cancelRollouts = cancelRollouts;
        this.cancelAutoAssignments = cancelAutoAssignments;
    }

    public long getSetId() {
        return setId;
    }

    public void setSetId(final long setId) {
        this.setId = setId;
    }

    public CancelationType getCancelationType() {
        return cancelationType;
    }

    public void setCancelationType(final CancelationType cancelationType) {
        this.cancelationType = cancelationType;
    }

    public boolean isCancelRollouts() {
        return cancelRollouts;
    }

    public void setCancelRollouts(final boolean cancelRollouts) {
        this.cancelRollouts = cancelRollouts;
    }

    public boolean isCancelAutoAssignments() {
        return cancelAutoAssignments;
    }

    public void setCancelAutoAssignments(final boolean cancelAutoAssignments) {
        this.cancelAutoAssignments = cancelAutoAssignments;
    }

    public enum CancelationType {
        FORCE, SOFT, NONE;
    }

}
