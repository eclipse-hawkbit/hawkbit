/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ui;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.hawkbit.eventbus.event.DistributionSetTagCreatedBulkEvent;
import org.eclipse.hawkbit.eventbus.event.DistributionSetTagDeletedEvent;
import org.eclipse.hawkbit.eventbus.event.DistributionSetTagUpdateEvent;
import org.eclipse.hawkbit.eventbus.event.Event;
import org.eclipse.hawkbit.eventbus.event.RolloutChangeEvent;
import org.eclipse.hawkbit.eventbus.event.RolloutGroupChangeEvent;
import org.eclipse.hawkbit.eventbus.event.TargetCreatedEvent;
import org.eclipse.hawkbit.eventbus.event.TargetDeletedEvent;
import org.eclipse.hawkbit.eventbus.event.TargetInfoUpdateEvent;
import org.eclipse.hawkbit.eventbus.event.TargetTagCreatedBulkEvent;

/**
 * The default hawkbit event provider.
 */
public class HawkbitEventProvider implements UIEventProvider {

    private static final Set<Class<? extends Event>> SINGLE_EVENTS = new HashSet<>();

    static {
        SINGLE_EVENTS.add(TargetTagCreatedBulkEvent.class);
        SINGLE_EVENTS.add(DistributionSetTagCreatedBulkEvent.class);
        SINGLE_EVENTS.add(DistributionSetTagDeletedEvent.class);
        SINGLE_EVENTS.add(DistributionSetTagUpdateEvent.class);
        SINGLE_EVENTS.add(RolloutGroupChangeEvent.class);
        SINGLE_EVENTS.add(RolloutChangeEvent.class);
    }

    private static final Set<Class<? extends Event>> BULK_EVENTS = new HashSet<>();

    static {
        BULK_EVENTS.add(TargetCreatedEvent.class);
        BULK_EVENTS.add(TargetInfoUpdateEvent.class);
        BULK_EVENTS.add(TargetDeletedEvent.class);
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
