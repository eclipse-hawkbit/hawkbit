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
 * View for querying {@link Action} include the count of the action's
 * {@link ActionStatus} entries.
 *
 */
public interface ActionWithStatusCount {

    /**
     * @return {@link Action}
     */
    Action getAction();

    /**
     * @return {@link DistributionSet} ID.
     */
    Long getDsId();

    /**
     * @return {@link DistributionSet} name.
     */
    String getDsName();

    /**
     * @return {@link DistributionSet} version.
     */
    String getDsVersion();

    /**
     * @return number of {@link ActionStatus} entries
     */
    Long getActionStatusCount();

    /**
     * @return name of the {@link Rollout}.
     */
    String getRolloutName();

}
