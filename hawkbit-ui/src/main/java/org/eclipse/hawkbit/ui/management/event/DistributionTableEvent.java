/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.event;

import org.eclipse.hawkbit.repository.model.DistributionSet;

/**
 *
 *
 *
 */
public class DistributionTableEvent {

    /**
     *
     *
     */
    public enum DistributionComponentEvent {
        ADD_DISTRIBUTION, EDIT_DISTRIBUTION, DELETE_DISTRIBUTION, ON_VALUE_CHANGE, MAXIMIZED, MINIMIZED
    }

    private DistributionComponentEvent distributionComponentEvent;

    private DistributionSet distributionSet;

    /**
     * @param distributionComponentEvent
     * @param distributionSet
     */
    public DistributionTableEvent(final DistributionComponentEvent distributionComponentEvent,
            final DistributionSet distributionSet) {
        this.distributionComponentEvent = distributionComponentEvent;
        this.distributionSet = distributionSet;
    }

    public DistributionComponentEvent getDistributionComponentEvent() {
        return distributionComponentEvent;
    }

    public void setDistributionComponentEvent(final DistributionComponentEvent distributionComponentEvent) {
        this.distributionComponentEvent = distributionComponentEvent;
    }

    public DistributionSet getDistributionSet() {
        return distributionSet;
    }

    public void setDistributionSet(final DistributionSet distributionSet) {
        this.distributionSet = distributionSet;
    }

}
