/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common;

import java.util.Map;

/**
 * Interface for event aware event listeners
 *
 */
public interface EventListenersAwareLayout {

    /**
     * Restore components state
     */
    default void restoreState() {
    }

    /**
     * Update components on view enter
     */
    default void onViewEnter() {
    }

    /**
     * Set components state based on url parameters
     * 
     * @param stateParameters
     *            map of state url parameters
     */
    default void handleStateParameters(final Map<String, String> stateParameters) {
    }

    /**
     * Subscribe event listeners
     */
    default void subscribeListeners() {
    }

    /**
     * Unsubscribe event listeners
     */
    default void unsubscribeListeners() {
    }
}
