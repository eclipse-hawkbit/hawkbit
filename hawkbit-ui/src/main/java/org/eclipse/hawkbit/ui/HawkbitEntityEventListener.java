/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayloadIdentifier;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.components.NotificationUnreadButton;
import org.eclipse.hawkbit.ui.push.UIEventProvider;
import org.springframework.util.CollectionUtils;
import org.vaadin.spring.events.Event;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventBusListenerMethodFilter;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.vaadin.ui.UI;

/**
 * Listener for internal and remote entity modified events. Keeps a cache of the
 * events coming from UI in order to suppress the corresponding remote events.
 */
public class HawkbitEntityEventListener {
    private final UIEventBus eventBus;
    private final UIEventProvider eventProvider;
    private final NotificationUnreadButton notificationUnreadButton;

    private final Cache<EntityModifiedEventPayloadIdentifier, Collection<Long>> uiOriginatedEventsCache;
    private final List<Object> eventListeners;

    HawkbitEntityEventListener(final UIEventBus eventBus, final UIEventProvider eventProvider,
            final NotificationUnreadButton notificationUnreadButton) {
        this.eventBus = eventBus;
        this.eventProvider = eventProvider;
        this.notificationUnreadButton = notificationUnreadButton;

        this.uiOriginatedEventsCache = Caffeine.newBuilder().expireAfterWrite(10, SECONDS).build();

        this.eventListeners = new ArrayList<>();
        registerEventListeners();
    }

    private void registerEventListeners() {
        eventListeners.add(new EntityModifiedListener());
        eventListeners.add(new RemoteEventListener());
    }

    private class EntityModifiedListener {

        /**
         * Constructor for EntityModifiedListener
         */
        public EntityModifiedListener() {
            eventBus.subscribe(this, EventTopics.ENTITY_MODIFIED);
        }

        @EventBusListenerMethod(scope = EventScope.UI, filter = IgnoreRemoteEventsFilter.class)
        private void onEntityModifiedEvent(final EntityModifiedEventPayload eventPayload) {
            // parentId is ignored here because entityIds should be unique
            uiOriginatedEventsCache.asMap().merge(EntityModifiedEventPayloadIdentifier.of(eventPayload),
                    eventPayload.getEntityIds(), (oldEntityIds, newEntityIds) -> Stream
                            .concat(oldEntityIds.stream(), newEntityIds.stream()).collect(Collectors.toList()));
        }
    }

    /**
     * Ignore remote events filter
     */
    public static class IgnoreRemoteEventsFilter implements EventBusListenerMethodFilter {

        @Override
        public boolean filter(final Event<?> event) {
            return !event.getSource().equals(UI.getCurrent());
        }
    }

    private class RemoteEventListener {

        /**
         * Constructor for RemoteEventListener
         */
        public RemoteEventListener() {
            eventBus.subscribe(this, EventTopics.REMOTE_EVENT_RECEIVED);
        }

        @EventBusListenerMethod(scope = EventScope.UI)
        private void onRemoteEventReceived(final EntityModifiedEventPayload eventPayload) {
            if (eventPayload.getEntityType() == null || eventPayload.getEntityModifiedEventType() == null
                    || CollectionUtils.isEmpty(eventPayload.getEntityIds())) {
                return;
            }

            getEventPayloadIdentifierFromProvider(eventPayload).ifPresent(eventPayloadIdentifier -> {
                final Collection<Long> remotelyModifiedEntityIds = getRemotelyModifiedEntityIds(eventPayloadIdentifier,
                        eventPayload.getEntityIds());

                if (!remotelyModifiedEntityIds.isEmpty()) {
                    final EntityModifiedEventPayload remoteEventPayload = EntityModifiedEventPayload
                            .of(eventPayloadIdentifier, eventPayload.getParentId(), remotelyModifiedEntityIds);

                    if (eventPayloadIdentifier.shouldBeDeffered()) {
                        notificationUnreadButton.incrementUnreadNotification(
                                eventPayloadIdentifier.getNotificationType(), remoteEventPayload);
                    } else {
                        eventBus.publish(EventTopics.ENTITY_MODIFIED, UI.getCurrent(), remoteEventPayload);
                    }
                }
            });
        }

        private Optional<EntityModifiedEventPayloadIdentifier> getEventPayloadIdentifierFromProvider(
                final EntityModifiedEventPayload eventPayload) {
            return eventProvider.getEvents().values().stream().filter(providedIdentifier -> providedIdentifier
                    .equals(EntityModifiedEventPayloadIdentifier.of(eventPayload))).findAny();
        }

        private Collection<Long> getRemotelyModifiedEntityIds(
                final EntityModifiedEventPayloadIdentifier eventPayloadIdentifier,
                final Collection<Long> remoteEventEntityIds) {
            final Collection<Long> cachedEventEntityIds = uiOriginatedEventsCache.getIfPresent(eventPayloadIdentifier);

            if (CollectionUtils.isEmpty(cachedEventEntityIds)) {
                return remoteEventEntityIds;
            }

            final Collection<Long> commonEntityIds = getCommonEntityIds(cachedEventEntityIds, remoteEventEntityIds);
            if (!commonEntityIds.isEmpty()) {
                updateCache(eventPayloadIdentifier, cachedEventEntityIds, commonEntityIds);
                remoteEventEntityIds.removeAll(commonEntityIds);
            }

            return remoteEventEntityIds;
        }

        private Collection<Long> getCommonEntityIds(final Collection<Long> cachedEventEntityIds,
                final Collection<Long> remoteEventEntityIds) {
            final List<Long> commonEntityIds = new ArrayList<>(cachedEventEntityIds);
            commonEntityIds.retainAll(remoteEventEntityIds);

            return commonEntityIds;
        }

        private void updateCache(final EntityModifiedEventPayloadIdentifier eventPayloadIdentifier,
                final Collection<Long> cachedEventEntityIds, final Collection<Long> commonEntityIds) {
            final List<Long> updatedCachedEventEntityIds = new ArrayList<>(cachedEventEntityIds);
            updatedCachedEventEntityIds.removeAll(commonEntityIds);

            if (updatedCachedEventEntityIds.isEmpty()) {
                uiOriginatedEventsCache.invalidate(eventPayloadIdentifier);
            } else {
                uiOriginatedEventsCache.put(eventPayloadIdentifier, updatedCachedEventEntityIds);
            }
        }
    }

    void unsubscribeListeners() {
        eventListeners.forEach(eventBus::unsubscribe);
    }
}
