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
 *
 *
 *
 */
public enum TargetFilterEvent {
    //
    FILTER_BY_TEXT,
    //
    FILTER_BY_TAG,
    //
    REMOVE_FILTER_BY_TEXT,
    //
    REMOVE_FILTER_BY_TAG,
    //
    FILTER_BY_STATUS,
    //
    FILTER_BY_DISTRIBUTION,
    //
    REMOVE_FILTER_BY_STATUS,
    //
    REMOVE_FILTER_BY_DISTRIBUTION,
    //
    FILTER_BY_TARGET_FILTER_QUERY,
    //
    REMOVE_FILTER_BY_TARGET_FILTER_QUERY
}
