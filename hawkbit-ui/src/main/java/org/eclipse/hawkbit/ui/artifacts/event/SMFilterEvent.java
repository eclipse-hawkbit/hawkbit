/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.event;

/**
 * Software module filter events. It is necessary to specify events for every
 * view, because otherwise the events are processed from every view which
 * catches the event. The result is, that the software module search is not
 * working correctly on the Distribution and Upload View.
 */
public enum SMFilterEvent {

    REMOVE_FILTER_BY_TYPE_DISTRIBUTION_VIEW,

    FILTER_BY_TYPE_DISTRIBUTION_VIEW,

    FILTER_BY_TEXT_DISTRIBUTION_VIEW,

    REMOVE_FILTER_BY_TEXT_DISTRIBUTION_VIEW,

    FILTER_BY_TEXT_UPLOAD_VIEW,

    REMOVE_FILTER_BY_TEXT_UPLOAD_VIEW,

    REMOVE_FILTER_BY_TYPE_UPLOAD_VIEW,

    FILTER_BY_TYPE_UPLOAD_VIEW,
}
