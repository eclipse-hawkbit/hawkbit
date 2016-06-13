/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.event;

import org.eclipse.hawkbit.repository.model.DistributionSetType;

/**
 * DistributionSetTypeEvent
 */
public class DistributionSetTypeEvent {

    /**
     * DistributionSet type events in the Distribution UI.
     */
    public enum DistributionSetTypeEnum {
        ADD_DIST_SET_TYPE, DELETE_DIST_SET_TYPE, UPDATE_DIST_SET_TYPE, ON_VALUE_CHANGE
    }

    private DistributionSetType distributionSetType;

    private final DistributionSetTypeEnum distributionSetTypeEnum;

    /**
     * @param distributionSetTypeEnum
     */
    public DistributionSetTypeEvent(final DistributionSetTypeEnum distributionSetTypeEnum) {
        this.distributionSetTypeEnum = distributionSetTypeEnum;
    }

    /**
     * @param distributionSetTypeEnum
     * @param distributionSetType
     */
    public DistributionSetTypeEvent(final DistributionSetTypeEnum distributionSetTypeEnum,
            final DistributionSetType distributionSetType) {
        this.distributionSetTypeEnum = distributionSetTypeEnum;
        this.distributionSetType = distributionSetType;
    }

    public DistributionSetType getDistributionSetType() {
        return distributionSetType;
    }

    public DistributionSetTypeEnum getDistributionSetTypeEnum() {
        return distributionSetTypeEnum;
    }

}
