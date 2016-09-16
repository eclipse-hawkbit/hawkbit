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

import org.eclipse.hawkbit.repository.event.Event;
import org.eclipse.hawkbit.repository.event.local.RolloutChangeEvent;
import org.eclipse.hawkbit.repository.event.local.RolloutGroupChangeEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetInfoUpdateEvent;
import org.eclipse.hawkbit.repository.event.remote.bulk.DistributionSetTagCreatedBulkEvent;
import org.eclipse.hawkbit.repository.event.remote.bulk.TargetTagCreatedBulkEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagUpdateEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdateEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagUpdateEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;

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
