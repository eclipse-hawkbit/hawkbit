/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

/**
 * The overall {@link TargetUpdateStatus} of a {@link Target} that describes its
 * status. A {@link Target} can have only one status. independent of the number
 * of {@link Action}s that have to be applied.
 *
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
    REGISTERED;
}
