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

import org.eclipse.hawkbit.repository.event.entity.EntityIdEvent;
import org.eclipse.hawkbit.repository.event.remote.DistributionSetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.DistributionSetTagDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.DistributionSetTypeDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.RolloutDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.SoftwareModuleDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.SoftwareModuleTypeDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetFilterQueryDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetTagDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetTypeDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTypeCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTypeUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutCreatedEvent;
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
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyAction;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRollout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRolloutGroup;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetFilterQuery;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetType;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayloadIdentifier;
import org.eclipse.hawkbit.ui.common.event.EventNotificationType;
import org.eclipse.hawkbit.ui.push.event.ActionChangedEvent;
import org.eclipse.hawkbit.ui.push.event.RolloutChangedEvent;
import org.eclipse.hawkbit.ui.push.event.RolloutGroupChangedEvent;

import com.google.common.collect.Maps;

/**
 * The default hawkbit event provider.
 */
public class HawkbitEventProvider implements UIEventProvider {

    private static final Map<Class<? extends EntityIdEvent>, EntityModifiedEventPayloadIdentifier> EVENTS = Maps
            .newHashMapWithExpectedSize(29);

    static {
        EVENTS.put(TargetCreatedEvent.class, new EntityModifiedEventPayloadIdentifier(ProxyTarget.class,
                EntityModifiedEventType.ENTITY_ADDED, EventNotificationType.TARGET_CREATED));
        EVENTS.put(TargetUpdatedEvent.class,
                new EntityModifiedEventPayloadIdentifier(ProxyTarget.class, EntityModifiedEventType.ENTITY_UPDATED));
        EVENTS.put(TargetDeletedEvent.class, new EntityModifiedEventPayloadIdentifier(ProxyTarget.class,
                EntityModifiedEventType.ENTITY_REMOVED, EventNotificationType.TARGET_DELETED));

        EVENTS.put(DistributionSetCreatedEvent.class,
                new EntityModifiedEventPayloadIdentifier(ProxyDistributionSet.class,
                        EntityModifiedEventType.ENTITY_ADDED, EventNotificationType.DISTRIBUTIONSET_CREATED));
        EVENTS.put(DistributionSetUpdatedEvent.class, new EntityModifiedEventPayloadIdentifier(
                ProxyDistributionSet.class, EntityModifiedEventType.ENTITY_UPDATED));
        EVENTS.put(DistributionSetDeletedEvent.class,
                new EntityModifiedEventPayloadIdentifier(ProxyDistributionSet.class,
                        EntityModifiedEventType.ENTITY_REMOVED, EventNotificationType.DISTRIBUTIONSET_DELETED));

        EVENTS.put(SoftwareModuleCreatedEvent.class, new EntityModifiedEventPayloadIdentifier(ProxySoftwareModule.class,
                EntityModifiedEventType.ENTITY_ADDED, EventNotificationType.SOFTWAREMODULE_CREATED));
        EVENTS.put(SoftwareModuleUpdatedEvent.class, new EntityModifiedEventPayloadIdentifier(ProxySoftwareModule.class,
                EntityModifiedEventType.ENTITY_UPDATED));
        EVENTS.put(SoftwareModuleDeletedEvent.class, new EntityModifiedEventPayloadIdentifier(ProxySoftwareModule.class,
                EntityModifiedEventType.ENTITY_REMOVED, EventNotificationType.SOFTWAREMODULE_DELETED));

        EVENTS.put(TargetTagCreatedEvent.class, new EntityModifiedEventPayloadIdentifier(ProxyTarget.class,
                ProxyTag.class, EntityModifiedEventType.ENTITY_ADDED));
        EVENTS.put(TargetTagUpdatedEvent.class, new EntityModifiedEventPayloadIdentifier(ProxyTarget.class,
                ProxyTag.class, EntityModifiedEventType.ENTITY_UPDATED));
        EVENTS.put(TargetTagDeletedEvent.class, new EntityModifiedEventPayloadIdentifier(ProxyTarget.class,
                ProxyTag.class, EntityModifiedEventType.ENTITY_REMOVED));

        EVENTS.put(DistributionSetTagCreatedEvent.class, new EntityModifiedEventPayloadIdentifier(
                ProxyDistributionSet.class, ProxyTag.class, EntityModifiedEventType.ENTITY_ADDED));
        EVENTS.put(DistributionSetTagUpdatedEvent.class, new EntityModifiedEventPayloadIdentifier(
                ProxyDistributionSet.class, ProxyTag.class, EntityModifiedEventType.ENTITY_UPDATED));
        EVENTS.put(DistributionSetTagDeletedEvent.class, new EntityModifiedEventPayloadIdentifier(
                ProxyDistributionSet.class, ProxyTag.class, EntityModifiedEventType.ENTITY_REMOVED));

        EVENTS.put(DistributionSetTypeCreatedEvent.class, new EntityModifiedEventPayloadIdentifier(
                ProxyDistributionSet.class, ProxyType.class, EntityModifiedEventType.ENTITY_ADDED));
        EVENTS.put(DistributionSetTypeUpdatedEvent.class, new EntityModifiedEventPayloadIdentifier(
                ProxyDistributionSet.class, ProxyType.class, EntityModifiedEventType.ENTITY_UPDATED));
        EVENTS.put(DistributionSetTypeDeletedEvent.class, new EntityModifiedEventPayloadIdentifier(
                ProxyDistributionSet.class, ProxyType.class, EntityModifiedEventType.ENTITY_REMOVED));

        EVENTS.put(SoftwareModuleTypeCreatedEvent.class, new EntityModifiedEventPayloadIdentifier(
                ProxySoftwareModule.class, ProxyType.class, EntityModifiedEventType.ENTITY_ADDED));
        EVENTS.put(SoftwareModuleTypeUpdatedEvent.class, new EntityModifiedEventPayloadIdentifier(
                ProxySoftwareModule.class, ProxyType.class, EntityModifiedEventType.ENTITY_UPDATED));
        EVENTS.put(SoftwareModuleTypeDeletedEvent.class, new EntityModifiedEventPayloadIdentifier(
                ProxySoftwareModule.class, ProxyType.class, EntityModifiedEventType.ENTITY_REMOVED));

        EVENTS.put(RolloutCreatedEvent.class,
                new EntityModifiedEventPayloadIdentifier(ProxyRollout.class, EntityModifiedEventType.ENTITY_ADDED));
        EVENTS.put(RolloutChangedEvent.class,
                new EntityModifiedEventPayloadIdentifier(ProxyRollout.class, EntityModifiedEventType.ENTITY_UPDATED));
        EVENTS.put(RolloutDeletedEvent.class,
                new EntityModifiedEventPayloadIdentifier(ProxyRollout.class, EntityModifiedEventType.ENTITY_REMOVED));

        EVENTS.put(RolloutGroupChangedEvent.class, new EntityModifiedEventPayloadIdentifier(ProxyRollout.class,
                ProxyRolloutGroup.class, EntityModifiedEventType.ENTITY_UPDATED));

        EVENTS.put(ActionChangedEvent.class, new EntityModifiedEventPayloadIdentifier(ProxyTarget.class,
                ProxyAction.class, EntityModifiedEventType.ENTITY_UPDATED));

        EVENTS.put(TargetFilterQueryCreatedEvent.class, new EntityModifiedEventPayloadIdentifier(
                ProxyTargetFilterQuery.class, EntityModifiedEventType.ENTITY_ADDED));
        EVENTS.put(TargetFilterQueryUpdatedEvent.class, new EntityModifiedEventPayloadIdentifier(
                ProxyTargetFilterQuery.class, EntityModifiedEventType.ENTITY_UPDATED));
        EVENTS.put(TargetFilterQueryDeletedEvent.class, new EntityModifiedEventPayloadIdentifier(
                ProxyTargetFilterQuery.class, EntityModifiedEventType.ENTITY_REMOVED));

        EVENTS.put(TargetTypeCreatedEvent.class, new EntityModifiedEventPayloadIdentifier(
                ProxyTargetType.class, EntityModifiedEventType.ENTITY_ADDED));
        EVENTS.put(TargetTypeUpdatedEvent.class, new EntityModifiedEventPayloadIdentifier(
                ProxyTargetType.class, EntityModifiedEventType.ENTITY_UPDATED));
        EVENTS.put(TargetTypeDeletedEvent.class, new EntityModifiedEventPayloadIdentifier(
                ProxyTargetType.class, EntityModifiedEventType.ENTITY_REMOVED));
    }

    @Override
    public Map<Class<? extends EntityIdEvent>, EntityModifiedEventPayloadIdentifier> getEvents() {
        return EVENTS;
    }
}
