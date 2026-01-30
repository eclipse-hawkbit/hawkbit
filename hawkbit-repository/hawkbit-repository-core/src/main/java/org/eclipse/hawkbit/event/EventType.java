/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.event;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.eclipse.hawkbit.repository.event.remote.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.DistributionSetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.DistributionSetTagDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.DistributionSetTypeDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.DownloadProgressEvent;
import org.eclipse.hawkbit.repository.event.remote.RolloutDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.RolloutGroupDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.RolloutStoppedEvent;
import org.eclipse.hawkbit.repository.event.remote.SoftwareModuleDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.SoftwareModuleTypeDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetFilterQueryDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetTagDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetTypeDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TenantConfigurationDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTypeCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTypeUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleTypeCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleTypeUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetFilterQueryCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetFilterQueryUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTypeCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTypeUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TenantConfigurationCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TenantConfigurationUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.service.ActionCreatedServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.ActionUpdatedServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.CancelTargetAssignmentServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetAssignDistributionSetServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetAttributesRequestedServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetCreatedServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetDeletedServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetUpdatedServiceEvent;

/**
 * The {@link EventType} class declares the event-type and it's corresponding
 * encoding value in the payload of an remote header. The event-type is encoded
 * into the payload of the message which is distributed.
 *
 * To encode and decode the event class type we need some conversation mapping
 * between the actual class and the corresponding integer value which is the
 * encoded value in the byte-payload.
 */
// for marshalling and unmarshalling.
@NoArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class EventType {

    private static final Map<Integer, Class<?>> TYPES = new HashMap<>();

    private int value;

    // The associated event-type-value must remain the same as initially
    // declared. Otherwise, messages cannot correctly de-serialized.
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
        TYPES.put(27, RolloutCreatedEvent.class);

        // distribution set type
        TYPES.put(28, DistributionSetTypeCreatedEvent.class);
        TYPES.put(29, DistributionSetTypeUpdatedEvent.class);
        TYPES.put(30, DistributionSetTypeDeletedEvent.class);

        // software module type
        TYPES.put(31, SoftwareModuleTypeCreatedEvent.class);
        TYPES.put(32, SoftwareModuleTypeUpdatedEvent.class);
        TYPES.put(33, SoftwareModuleTypeDeletedEvent.class);

        // target filter query
        TYPES.put(34, TargetFilterQueryCreatedEvent.class);
        TYPES.put(35, TargetFilterQueryUpdatedEvent.class);
        TYPES.put(36, TargetFilterQueryDeletedEvent.class);

        // target attributes requested flag
        TYPES.put(37, TargetAttributesRequestedEvent.class);


        // tenant configuration
        TYPES.put(40, TenantConfigurationCreatedEvent.class);
        TYPES.put(41, TenantConfigurationUpdatedEvent.class);
        TYPES.put(42, TenantConfigurationDeletedEvent.class);

        // rollout stopped due to invalidated distribution set
        TYPES.put(43, RolloutStoppedEvent.class);

        // target type
        TYPES.put(44, TargetTypeCreatedEvent.class);
        TYPES.put(45, TargetTypeUpdatedEvent.class);
        TYPES.put(46, TargetTypeDeletedEvent.class);

        // processing events - start from 1000 to leave room for future db events
        TYPES.put(1000, TargetCreatedServiceEvent.class);
        TYPES.put(1001, TargetUpdatedServiceEvent.class);
        TYPES.put(1002, TargetDeletedServiceEvent.class);
        TYPES.put(1003, TargetAssignDistributionSetServiceEvent.class);
        TYPES.put(1004, TargetAttributesRequestedServiceEvent.class);
        TYPES.put(1005, CancelTargetAssignmentServiceEvent.class);
        TYPES.put(1008, ActionCreatedServiceEvent.class);
        TYPES.put(1009, ActionUpdatedServiceEvent.class);
    }

    /**
     * Constructor.
     *
     * @param value the value to initialize
     */
    public EventType(final int value) {
        this.value = value;
    }

    /**
     * Returns a {@link EventType} based on the given class type.
     *
     * @param clazz the clazz type to retrieve the corresponding {@link EventType}
     *         .
     * @return the corresponding {@link EventType} or {@code null} if the clazz
     *         does not have a {@link EventType}.
     */
    public static EventType from(final Class<?> clazz) {
        return TYPES.entrySet().stream()
                .filter(entry -> entry.getValue().equals(clazz))
                .map(Entry::getKey)
                .findAny()
                .map(EventType::new)
                .orElse(null);
    }

    public Class<?> getTargetClass() {
        return TYPES.get(value);
    }

    public static Collection<NamedType> getNamedTypes() {
        return TYPES.entrySet().stream()
                .map(e -> new NamedType(e.getValue(), String.valueOf(e.getKey())))
                .toList();
    }
}