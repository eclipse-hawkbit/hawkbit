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
import org.eclipse.hawkbit.ui.push.events.CancelTargetAssignmentEventHolder;
import org.eclipse.hawkbit.ui.push.events.DistributionCreatedEventHolder;
import org.eclipse.hawkbit.ui.push.events.DistributionDeletedEventHolder;
import org.eclipse.hawkbit.ui.push.events.DistributionSetTagAssignmentResultEventHolder;
import org.eclipse.hawkbit.ui.push.events.DistributionSetTagCreatedEventHolder;
import org.eclipse.hawkbit.ui.push.events.DistributionSetTagDeletedEventHolder;
import org.eclipse.hawkbit.ui.push.events.DistributionSetTagUpdatedEventHolder;
import org.eclipse.hawkbit.ui.push.events.DistributionSetUpdatedEventHolder;
import org.eclipse.hawkbit.ui.push.events.RolloutChangeEventHolder;
import org.eclipse.hawkbit.ui.push.events.RolloutGroupChangeEventHolder;
import org.eclipse.hawkbit.ui.push.events.TargetCreatedEventHolder;
import org.eclipse.hawkbit.ui.push.events.TargetDeletedEventHolder;
import org.eclipse.hawkbit.ui.push.events.TargetInfoUpdateEventHolder;
import org.eclipse.hawkbit.ui.push.events.TargetTagAssigmentResultEventHolder;
import org.eclipse.hawkbit.ui.push.events.TargetTagCreatedEventHolder;
import org.eclipse.hawkbit.ui.push.events.TargetTagDeletedEventHolder;
import org.eclipse.hawkbit.ui.push.events.TargetTagUpdatedEventHolder;
import org.eclipse.hawkbit.ui.push.events.TargetUpdatedEventHolder;

import com.google.gwt.thirdparty.guava.common.collect.Maps;

/**
 * The default hawkbit event provider.
 */
public class HawkbitEventProvider implements UIEventProvider {

    private static final Map<Class<? extends Event>, Class<?>> EVENTS = Maps.newHashMapWithExpectedSize(18);

    static {

        EVENTS.put(TargetTagDeletedEvent.class, TargetTagDeletedEventHolder.class);
        EVENTS.put(TargetTagCreatedEvent.class, TargetTagCreatedEventHolder.class);
        EVENTS.put(TargetTagUpdateEvent.class, TargetTagUpdatedEventHolder.class);
        EVENTS.put(TargetTagAssigmentResultEvent.class, TargetTagAssigmentResultEventHolder.class);

        EVENTS.put(DistributionSetTagCreatedEvent.class, DistributionSetTagCreatedEventHolder.class);
        EVENTS.put(DistributionSetTagDeletedEvent.class, DistributionSetTagDeletedEventHolder.class);
        EVENTS.put(DistributionSetTagUpdateEvent.class, DistributionSetTagUpdatedEventHolder.class);
        EVENTS.put(DistributionSetTagAssigmentResultEvent.class, DistributionSetTagAssignmentResultEventHolder.class);

        EVENTS.put(TargetCreatedEvent.class, TargetCreatedEventHolder.class);
        EVENTS.put(TargetInfoUpdateEvent.class, TargetInfoUpdateEventHolder.class);
        EVENTS.put(TargetDeletedEvent.class, TargetDeletedEventHolder.class);
        EVENTS.put(TargetUpdatedEvent.class, TargetUpdatedEventHolder.class);
        EVENTS.put(CancelTargetAssignmentEvent.class, CancelTargetAssignmentEventHolder.class);

        EVENTS.put(DistributionSetUpdateEvent.class, DistributionSetUpdatedEventHolder.class);
        EVENTS.put(DistributionDeletedEvent.class, DistributionDeletedEventHolder.class);
        EVENTS.put(DistributionCreatedEvent.class, DistributionCreatedEventHolder.class);

        EVENTS.put(RolloutGroupChangeEvent.class, RolloutGroupChangeEventHolder.class);
        EVENTS.put(RolloutChangeEvent.class, RolloutChangeEventHolder.class);

    }

    @Override
    public Map<Class<? extends Event>, Class<?>> getEvents() {
        return EVENTS;
    }

}
