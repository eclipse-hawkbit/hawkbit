/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.push.events;

import java.util.List;

import org.eclipse.hawkbit.repository.eventbus.event.RolloutChangeEvent;
import org.eclipse.hawkbit.ui.push.EventHolder;

public class RolloutChangeEventHolder implements EventHolder {
    private final List<RolloutChangeEvent> events;

    public RolloutChangeEventHolder(final List<RolloutChangeEvent> events) {
        this.events = events;
    }

    @Override
    public List<RolloutChangeEvent> getEvents() {
        return events;
    }

}
