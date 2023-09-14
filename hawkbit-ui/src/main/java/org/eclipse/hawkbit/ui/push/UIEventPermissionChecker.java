/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.push;

import org.eclipse.hawkbit.repository.event.entity.EntityIdEvent;

/**
 * The UI event permission checker verifies permissions of all supported
 * repository events which will be delegated to the UI.
 */
@FunctionalInterface
public interface UIEventPermissionChecker {

    /**
     * Checks if the event is allowed based on the repository event class
     * 
     * @param eventClass
     *            Event type
     * 
     * @return {@code true}: if event is allowed {@code false}: otherwise
     */
    boolean isEventAllowed(Class<? extends EntityIdEvent> eventClass);
}
