/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.hawkbit.eventbus.event.Event;
import org.eclipse.hawkbit.repository.eventbus.event.RolloutChangeEvent;
import org.eclipse.hawkbit.repository.eventbus.event.RolloutGroupChangeEvent;
import org.springframework.cloud.bus.event.bulk.DistributionSetTagCreatedBulkEvent;
import org.springframework.cloud.bus.event.bulk.TargetTagCreatedBulkEvent;
import org.springframework.cloud.bus.event.entity.DistributionCreatedEvent;
import org.springframework.cloud.bus.event.entity.DistributionDeletedEvent;
import org.springframework.cloud.bus.event.entity.DistributionSetTagDeletedEvent;
import org.springframework.cloud.bus.event.entity.DistributionSetTagUpdateEvent;
import org.springframework.cloud.bus.event.entity.DistributionSetUpdateEvent;
import org.springframework.cloud.bus.event.entity.TargetCreatedEvent;
import org.springframework.cloud.bus.event.entity.TargetDeletedEvent;
import org.springframework.cloud.bus.event.entity.TargetInfoUpdateEvent;
import org.springframework.cloud.bus.event.entity.TargetTagDeletedEvent;
import org.springframework.cloud.bus.event.entity.TargetTagUpdateEvent;
import org.springframework.cloud.bus.event.entity.TargetUpdatedEvent;

/**
 * The default hawkbit event provider.
 */
public class HawkbitEventProvider implements UIEventProvider {

    private static final Set<Class<? extends Event>> SINGLE_EVENTS = new HashSet<>(9);
    private static final Set<Class<? extends Event>> BULK_EVENTS = new HashSet<>(5);

    static {
        SINGLE_EVENTS.add(TargetTagCreatedBulkEvent.class);
        SINGLE_EVENTS.add(DistributionSetTagCreatedBulkEvent.class);
        SINGLE_EVENTS.add(DistributionSetTagDeletedEvent.class);
        SINGLE_EVENTS.add(TargetTagDeletedEvent.class);
        SINGLE_EVENTS.add(DistributionSetTagUpdateEvent.class);
        SINGLE_EVENTS.add(RolloutGroupChangeEvent.class);
        SINGLE_EVENTS.add(RolloutChangeEvent.class);
        SINGLE_EVENTS.add(TargetTagUpdateEvent.class);
        SINGLE_EVENTS.add(DistributionSetUpdateEvent.class);

        BULK_EVENTS.add(TargetCreatedEvent.class);
        BULK_EVENTS.add(TargetInfoUpdateEvent.class);
        BULK_EVENTS.add(TargetDeletedEvent.class);
        BULK_EVENTS.add(DistributionDeletedEvent.class);
        BULK_EVENTS.add(DistributionCreatedEvent.class);
        BULK_EVENTS.add(TargetUpdatedEvent.class);
    }

    @Override
    public Set<Class<? extends Event>> getSingleEvents() {
        return SINGLE_EVENTS;
    }

    @Override
    public Set<Class<? extends Event>> getBulkEvents() {
        return BULK_EVENTS;
    }

}
