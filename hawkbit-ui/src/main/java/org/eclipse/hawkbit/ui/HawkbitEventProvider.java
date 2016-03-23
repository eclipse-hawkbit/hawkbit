/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ui;

import java.util.Set;

import org.eclipse.hawkbit.eventbus.event.DistributionSetTagCreatedBulkEvent;
import org.eclipse.hawkbit.eventbus.event.DistributionSetTagDeletedEvent;
import org.eclipse.hawkbit.eventbus.event.DistributionSetTagUpdateEvent;
import org.eclipse.hawkbit.eventbus.event.RolloutChangeEvent;
import org.eclipse.hawkbit.eventbus.event.RolloutGroupChangeEvent;
import org.eclipse.hawkbit.eventbus.event.TargetCreatedEvent;
import org.eclipse.hawkbit.eventbus.event.TargetDeletedEvent;
import org.eclipse.hawkbit.eventbus.event.TargetInfoUpdateEvent;
import org.eclipse.hawkbit.eventbus.event.TargetTagCreatedBulkEvent;

import com.google.common.collect.Sets;

/**
 * @author Dennis Melzer
 *
 */
public class HawkbitEventProvider implements EventProvider {

    private static final Set<Class<?>> SINGLE_EVENTS = Sets.newHashSet(TargetTagCreatedBulkEvent.class,
            DistributionSetTagCreatedBulkEvent.class, DistributionSetTagDeletedEvent.class,
            DistributionSetTagUpdateEvent.class, RolloutGroupChangeEvent.class, RolloutChangeEvent.class);

    private static final Set<Class<?>> BULD_EVENTS = Sets.newHashSet(TargetCreatedEvent.class,
            TargetInfoUpdateEvent.class, TargetDeletedEvent.class);

    @Override
    public Set<Class<?>> getSingleEvents() {
        return SINGLE_EVENTS;
    }

    @Override
    public Set<Class<?>> getBulkEvents() {
        return BULD_EVENTS;
    }

}
