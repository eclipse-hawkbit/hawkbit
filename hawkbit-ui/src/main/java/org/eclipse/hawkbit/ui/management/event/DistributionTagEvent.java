/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.event;

import org.eclipse.hawkbit.repository.model.DistributionSetTag;

/**
 *
 *
 *
 */
public class DistributionTagEvent {

    /**
     *
     *
     */
    public enum DistTagComponentEvent {

        ADD_DIST_TAG, EDIT_DIST_TAG, DELETE_DIST_TAG, ASSIGNED, UNASSIGNED
    }

    private DistTagComponentEvent distTagComponentEvent;

    private DistributionSetTag distributionTag;

    private String distributionTagName;

    /**
     * @param distTagComponentEvent
     * @param distributionTag
     */
    public DistributionTagEvent(final DistTagComponentEvent distTagComponentEvent,
            final DistributionSetTag distributionTag) {
        this.distTagComponentEvent = distTagComponentEvent;
        this.distributionTag = distributionTag;

    }

    /**
     * @param distTagComponentEvent
     * @param distributionTagName
     */
    public DistributionTagEvent(final DistTagComponentEvent distTagComponentEvent, final String distributionTagName) {
        this.distTagComponentEvent = distTagComponentEvent;
        this.distributionTagName = distributionTagName;

    }

    public DistTagComponentEvent getDistTagComponentEvent() {
        return distTagComponentEvent;
    }

    public String getDistributionTagName() {
        return distributionTagName;
    }

    public void setDistTagComponentEvent(final DistTagComponentEvent distTagComponentEvent) {
        this.distTagComponentEvent = distTagComponentEvent;
    }

    public DistributionSetTag getDistributionTag() {
        return distributionTag;
    }

    public void setDistributionTag(final DistributionSetTag distributionTag) {
        this.distributionTag = distributionTag;
    }

}
