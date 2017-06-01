/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.event;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.hawkbit.repository.event.remote.DistributionSetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.DistributionSetTagDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.DownloadProgressEvent;
import org.eclipse.hawkbit.repository.event.remote.RolloutDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.RolloutGroupDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.SoftwareModuleDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetTagDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;

/**
 * The {@link EventType} class declares the event-type and it's corresponding
 * encoding value in the payload of an remote header. The event-type is encoded
 * into the payload of the message which is distributed.
 * 
 * To encode and decode the event class type we need some conversation mapping
 * between the actual class and the corresponding integer value which is the
 * encoded value in the byte-payload.
 */
public class EventType {

    private static final Map<Integer, Class<?>> TYPES = new HashMap<>();

    /**
     * The associated event-type-value must remain the same as initially
     * declared. Otherwise messages cannot correctly de-serialized.
     */
    static {

        // target
        TYPES.put(1, TargetCreatedEvent.class);
        TYPES.put(2, TargetUpdatedEvent.class);
        TYPES.put(3, TargetDeletedEvent.class);
        TYPES.put(4, CancelTargetAssignmentEvent.class);
        TYPES.put(5, TargetAssignDistributionSetEvent.class);

        // target tag
        TYPES.put(6, TargetTagCreatedEvent.class);
        TYPES.put(7, TargetTagUpdatedEvent.class);
        TYPES.put(8, TargetTagDeletedEvent.class);

        // action
        TYPES.put(9, ActionCreatedEvent.class);
        TYPES.put(10, ActionUpdatedEvent.class);

        // distribution set
        TYPES.put(11, DistributionSetCreatedEvent.class);
        TYPES.put(12, DistributionSetUpdatedEvent.class);
        TYPES.put(13, DistributionSetDeletedEvent.class);

        // distribution set tag
        TYPES.put(14, DistributionSetTagCreatedEvent.class);
        TYPES.put(15, DistributionSetTagUpdatedEvent.class);
        TYPES.put(16, DistributionSetTagDeletedEvent.class);

        // rollout
        TYPES.put(17, RolloutUpdatedEvent.class);

        // rollout group
        TYPES.put(18, RolloutGroupCreatedEvent.class);
        TYPES.put(19, RolloutGroupUpdatedEvent.class);

        // download
        TYPES.put(20, DownloadProgressEvent.class);

        TYPES.put(21, SoftwareModuleCreatedEvent.class);
        TYPES.put(22, SoftwareModuleDeletedEvent.class);
        TYPES.put(23, SoftwareModuleUpdatedEvent.class);

        TYPES.put(24, TargetPollEvent.class);
        TYPES.put(25, RolloutDeletedEvent.class);
        TYPES.put(26, RolloutGroupDeletedEvent.class);
    }

    private int value;

    /**
     * Constructor.
     */
    public EventType() {
        // for marshalling and unmarshalling.
    }

    /**
     * Constructor.
     * 
     * @param value
     *            the value to initialize
     */
    public EventType(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public Class<?> getTargetClass() {
        return TYPES.get(value);
    }

    /**
     * Returns a {@link EventType} based on the given class type.
     * 
     * @param clazz
     *            the clazz type to retrieve the corresponding {@link EventType}
     *            .
     * @return the corresponding {@link EventType} or {@code null} if the clazz
     *         does not have a {@link EventType}.
     */
    public static EventType from(final Class<?> clazz) {
        final Optional<Integer> foundEventType = TYPES.entrySet().stream()
                .filter(entry -> entry.getValue().equals(clazz)).map(entry -> entry.getKey()).findAny();
        if (!foundEventType.isPresent()) {
            return null;
        }
        return new EventType(foundEventType.get());
    }
}
