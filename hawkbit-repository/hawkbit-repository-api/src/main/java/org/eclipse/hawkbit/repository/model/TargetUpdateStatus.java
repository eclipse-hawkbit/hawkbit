/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

/**
 * The overall {@link TargetUpdateStatus} of a {@link Target} that describes its
 * status. A {@link Target} can have only one status. independent of the number
 * of {@link Action}s that have to be applied.
 */
public enum TargetUpdateStatus {

    /**
     * Set by default to targets until first status update received from the
     * target.
     */
    UNKNOWN,

    /**
     * Assigned {@link DistributionSet} is installed.
     */
    IN_SYNC,

    /**
     * Installation of assigned {@link DistributionSet} is no yet confirmed.
     */
    PENDING,

    /**
     * Installation of assigned {@link DistributionSet} has failed.
     */
    ERROR,

    /**
     * Controller registered at SP but no {@link DistributionSet} assigned.
     */
    REGISTERED
}