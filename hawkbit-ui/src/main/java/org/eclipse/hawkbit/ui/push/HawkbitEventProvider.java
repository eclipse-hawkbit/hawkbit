/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.push;

import java.util.Map;

import org.eclipse.hawkbit.eventbus.event.Event;
import org.eclipse.hawkbit.repository.eventbus.event.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.eventbus.event.DistributionCreatedEvent;
import org.eclipse.hawkbit.repository.eventbus.event.DistributionDeletedEvent;
import org.eclipse.hawkbit.repository.eventbus.event.DistributionSetTagAssigmentResultEvent;
import org.eclipse.hawkbit.repository.eventbus.event.DistributionSetTagCreatedEvent;
import org.eclipse.hawkbit.repository.eventbus.event.DistributionSetTagDeletedEvent;
import org.eclipse.hawkbit.repository.eventbus.event.DistributionSetTagUpdateEvent;
import org.eclipse.hawkbit.repository.eventbus.event.DistributionSetUpdateEvent;
import org.eclipse.hawkbit.repository.eventbus.event.RolloutChangeEvent;
import org.eclipse.hawkbit.repository.eventbus.event.RolloutGroupChangeEvent;
import org.eclipse.hawkbit.repository.eventbus.event.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.eventbus.event.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.eventbus.event.TargetInfoUpdateEvent;
import org.eclipse.hawkbit.repository.eventbus.event.TargetTagAssigmentResultEvent;
import org.eclipse.hawkbit.repository.eventbus.event.TargetTagCreatedEvent;
import org.eclipse.hawkbit.repository.eventbus.event.TargetTagDeletedEvent;
import org.eclipse.hawkbit.repository.eventbus.event.TargetTagUpdateEvent;
import org.eclipse.hawkbit.repository.eventbus.event.TargetUpdatedEvent;

import com.google.common.collect.Maps;

/**
 * The default hawkbit event provider.
 */
public class HawkbitEventProvider implements UIEventProvider {

    private static final Map<Class<? extends Event>, Class<?>> EVENTS = Maps.newHashMapWithExpectedSize(18);

    static {

        EVENTS.put(TargetTagDeletedEvent.class, TargetTagDeletedEventContainer.class);
        EVENTS.put(TargetTagCreatedEvent.class, TargetTagCreatedEventContainer.class);
        EVENTS.put(TargetTagUpdateEvent.class, TargetTagUpdatedEventContainer.class);
        EVENTS.put(TargetTagAssigmentResultEvent.class, TargetTagAssigmentResultEventContainer.class);

        EVENTS.put(DistributionSetTagCreatedEvent.class, DistributionSetTagCreatedEventContainer.class);
        EVENTS.put(DistributionSetTagDeletedEvent.class, DistributionSetTagDeletedEventContainer.class);
        EVENTS.put(DistributionSetTagUpdateEvent.class, DistributionSetTagUpdatedEventContainer.class);
        EVENTS.put(DistributionSetTagAssigmentResultEvent.class,
                DistributionSetTagAssignmentResultEventContainer.class);

        EVENTS.put(TargetCreatedEvent.class, TargetCreatedEventContainer.class);
        EVENTS.put(TargetInfoUpdateEvent.class, TargetInfoUpdateEventContainer.class);
        EVENTS.put(TargetDeletedEvent.class, TargetDeletedEventContainer.class);
        EVENTS.put(TargetUpdatedEvent.class, TargetUpdatedEventContainer.class);
        EVENTS.put(CancelTargetAssignmentEvent.class, CancelTargetAssignmentEventContainer.class);

        EVENTS.put(DistributionSetUpdateEvent.class, DistributionSetUpdatedEventContainer.class);
        EVENTS.put(DistributionDeletedEvent.class, DistributionDeletedEventContainer.class);
        EVENTS.put(DistributionCreatedEvent.class, DistributionCreatedEventContainer.class);

        EVENTS.put(RolloutGroupChangeEvent.class, RolloutGroupChangeEventContainer.class);
        EVENTS.put(RolloutChangeEvent.class, RolloutChangeEventContainer.class);

    }

    @Override
    public Map<Class<? extends Event>, Class<?>> getEvents() {
        return EVENTS;
    }

}
