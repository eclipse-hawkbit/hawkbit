/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.Collection;

/**
 * Holds the information about the invalidation of a distribution set
 */
public class DistributionSetInvalidation {

    private Collection<Long> distributionSetIds;
    private CancelationType cancelationType;
    private boolean cancelRollouts;

    /**
     * Parametric constructor
     *
     * @param distributionSetIds
     *            defines which distribution sets should be canceled
     * @param cancelationType
     *            defines if actions should be canceled
     * @param cancelRollouts
     *            defines if rollouts should be canceled
     */
    public DistributionSetInvalidation(final Collection<Long> distributionSetIds, final CancelationType cancelationType,
            final boolean cancelRollouts) {
        this.distributionSetIds = distributionSetIds;
        this.cancelationType = cancelationType;
        this.cancelRollouts = cancelRollouts;
    }

    public Collection<Long> getDistributionSetIds() {
        return distributionSetIds;
    }

    public void setDistributionSetIds(final Collection<Long> distributionSetIds) {
        this.distributionSetIds = distributionSetIds;
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

    /**
     * Defines if and how actions should be canceled when invalidating a
     * distribution set
     */
    public enum CancelationType {
        FORCE, SOFT, NONE
    }

}
