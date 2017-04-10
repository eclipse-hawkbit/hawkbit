/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.event;

/**
 * Distribution Set Filter Events. It is necessary to specify events for every
 * view, because otherwise the events are processed from every view which
 * catches the event. The result is, that the distribution set search is not
 * working correctly on the Deployment and Distribution View.
 */
public enum DistributionTableFilterEvent {

    FILTER_BY_TAG_DEPLOYMENT_VIEW,

    REMOVE_FILTER_BY_TAG_DEPLOYMENT_VIEW,

    FILTER_BY_TEXT_DEPLOYMENT_VIEW,

    REMOVE_FILTER_BY_TEXT_DEPLOYMENT_VIEW,

    FILTER_BY_TEXT_DISTRIBUTION_VIEW,

    REMOVE_FILTER_BY_TEXT_DISTRIBUTION_VIEW,

    FILTER_BY_TAG_DISTRIBUTION_VIEW,

    REMOVE_FILTER_BY_TAG_DISTRIBUTION_VIEW,

}
