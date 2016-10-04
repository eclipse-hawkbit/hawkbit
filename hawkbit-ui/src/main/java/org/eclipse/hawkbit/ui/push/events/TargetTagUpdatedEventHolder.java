/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.push.events;

import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.repository.eventbus.event.TargetTagUpdateEvent;
import org.eclipse.hawkbit.ui.push.EventHolder;

public class TargetTagUpdatedEventHolder implements EventHolder {

    public TargetTagUpdatedEventHolder(final List<TargetTagUpdateEvent> events) {
        // Events not needed by UI, i.e. a "Ping" is sufficient
    }

    @Override
    public List<TargetTagUpdateEvent> getEvents() {
        return Collections.emptyList();
    }

}
