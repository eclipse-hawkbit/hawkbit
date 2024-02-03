/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

import lombok.Data;

import java.util.Collection;

/**
 * Holds the information about the invalidation of a distribution set
 */
@Data
public class DistributionSetInvalidation {

    /**
     * Defines if and how actions should be canceled when invalidating a
     * distribution set
     */
    public enum CancelationType {
        FORCE, SOFT, NONE
    }

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
}