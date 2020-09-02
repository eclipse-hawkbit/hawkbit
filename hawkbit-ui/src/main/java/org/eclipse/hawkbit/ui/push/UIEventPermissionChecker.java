/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
