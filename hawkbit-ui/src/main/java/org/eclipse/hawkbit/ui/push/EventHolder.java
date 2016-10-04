/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.push;

import java.util.List;

import org.eclipse.hawkbit.eventbus.event.Event;

/**
 * EventHolder beans contains a list of events that can be process by the UI in
 * batch like fashion.
 *
 */
@FunctionalInterface
public interface EventHolder {

    /**
     * @return list of contained events
     */
    List<? extends Event> getEvents();

}
