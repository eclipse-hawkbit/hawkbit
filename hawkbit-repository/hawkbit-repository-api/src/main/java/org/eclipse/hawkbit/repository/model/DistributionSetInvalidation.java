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

import java.util.Collection;

import lombok.Data;

/**
 * Holds the information about the invalidation of a distribution set
 */
@Data
public class DistributionSetInvalidation {

    private Collection<Long> distributionSetIds;
    private ActionCancellationType actionCancellationType;

    /**
     * Parametric constructor
     *
     * @param distributionSetIds defines which distribution sets should be canceled
     * @param actionCancellationType defines if actions should be canceled
     */
    public DistributionSetInvalidation(final Collection<Long> distributionSetIds, final ActionCancellationType actionCancellationType) {
        this.distributionSetIds = distributionSetIds;
        this.actionCancellationType = actionCancellationType;
    }
}