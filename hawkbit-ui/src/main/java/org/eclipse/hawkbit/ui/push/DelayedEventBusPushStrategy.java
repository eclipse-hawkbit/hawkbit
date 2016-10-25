/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.push;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.repository.event.TenantAwareEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutUpdatedEvent;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.ui.push.event.RolloutChangeEvent;
import org.eclipse.hawkbit.ui.push.event.RolloutGroupChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.SessionEventBus;

import com.vaadin.server.VaadinSession;
import com.vaadin.server.VaadinSession.State;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.UI;

/**
 * An {@link EventPushStrategy} implementation which retrieves events from
 * {@link com.google.common.eventbus.EventBus} and store them first in a queue
 * where they will dispatched every 2 seconds to the {@link EventBus} in a
 * Vaadin access thread {@link UI#access(Runnable)}.
 *
 * This strategy avoids blocking UIs when too many events are fired and
 * dispatched to the UI thread. The UI will freeze in the time. To avoid that
 * all events are collected first and same events are merged to a list of events
 * before they dispatched to the UI thread.
 *
 * The strategy also verifies the current tenant in the session with the tenant
 * in the event and only forwards event from the right tenant to the UI.
 *
 */
public class DelayedEventBusPushStrategy implements EventPushStrategy, ApplicationListener<ApplicationEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(DelayedEventBusPushStrategy.class);

    private static final int BLOCK_SIZE = 10_000;
    private final BlockingDeque<org.eclipse.hawkbit.repository.event.TenantAwareEvent> queue = new LinkedBlockingDeque<>(
            BLOCK_SIZE);
    private int uiid = -1;

    @Autowired
    private ScheduledExecutorService executorService;

    @Autowired
    private EventBus.SessionEventBus eventBus;

    @Autowired
    private UIEventProvider eventProvider;
    private ScheduledFuture<?> jobHandle;

    private static final Set<RolloutEventKey> rolloutEvents = ConcurrentHashMap.newKeySet();
    private static final Set<RolloutEventKey> rolloutGroupEvents = ConcurrentHashMap.newKeySet();

    private boolean isEventProvided(final org.eclipse.hawkbit.repository.event.TenantAwareEvent event) {
        return eventProvider.getEvents().containsKey(event.getClass());
    }

    @Override
    public void init(final UI vaadinUI) {
        uiid = vaadinUI.getUIId();
        LOG.info("Initialize delayed event push strategy for UI {}", uiid);
        if (vaadinUI.getSession() == null) {
            LOG.error("Vaadin session of UI {} is null! Event push disabled!", uiid);
        }

        jobHandle = executorService.scheduleWithFixedDelay(new DispatchRunnable(vaadinUI, vaadinUI.getSession()),
                10_000, 1_000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void clean() {
        LOG.info("Cleanup delayed event push strategy for UI", uiid);
        jobHandle.cancel(true);
        queue.clear();
    }

    private final class DispatchRunnable implements Runnable {

        private final UI vaadinUI;
        private final VaadinSession vaadinSession;

        private DispatchRunnable(final UI ui, final VaadinSession session) {
            vaadinUI = ui;
            vaadinSession = session;
        }

        @Override
        public void run() {
            LOG.debug("UI EventBus aggregator started for UI {}", vaadinUI.getUIId());
            final long timestamp = System.currentTimeMillis();

            final Optional<RolloutChangeEvent> rolloutChangeEvent = getRolloutChangeEvent();
            final Optional<RolloutGroupChangeEvent> rolloutGroupChangeEvent = getRolloutGroupChangeEvent();

            final int size = calculateQueueSize(rolloutChangeEvent, rolloutGroupChangeEvent);
            if (size <= 0) {
                LOG.debug("UI EventBus aggregator for UI {} has nothing to do.", vaadinUI.getUIId());
                return;
            }

            final WrappedSession wrappedSession = vaadinSession.getSession();
            if (wrappedSession == null) {
                return;
            }

            final List<TenantAwareEvent> events = new ArrayList<>(size);
            final int eventsSize = queue.drainTo(events);

            addRolloutChangeEvents(rolloutChangeEvent, rolloutGroupChangeEvent, events);

            if (events.isEmpty()) {
                LOG.debug("UI EventBus aggregator for UI {} has nothing to do.", vaadinUI.getUIId());
                return;
            }

            LOG.debug("UI EventBus aggregator dispatches {} events for session {} for UI {}", eventsSize, vaadinSession,
                    vaadinUI.getUIId());

            doDispatch(events, wrappedSession);

            LOG.debug("UI EventBus aggregator done with sending {} events in {} ms for UI {}", eventsSize,
                    System.currentTimeMillis() - timestamp, vaadinUI.getUIId());

        }

        private void addRolloutChangeEvents(final Optional<RolloutChangeEvent> rolloutChangeEvent,
                final Optional<RolloutGroupChangeEvent> rolloutGroupChangeEvent, final List<TenantAwareEvent> events) {
            rolloutChangeEvent.ifPresent(events::add);
            rolloutGroupChangeEvent.ifPresent(events::add);

        }

        private Optional<RolloutChangeEvent> getRolloutChangeEvent() {
            final Iterator<RolloutEventKey> rolloutIterator = rolloutEvents.iterator();
            if (!rolloutIterator.hasNext()) {
                return Optional.empty();
            }
            final RolloutEventKey eventKey = rolloutIterator.next();
            rolloutEvents.clear();
            return Optional.of(new RolloutChangeEvent(eventKey.tenant, eventKey.rolloutId));
        }

        private Optional<RolloutGroupChangeEvent> getRolloutGroupChangeEvent() {
            final Iterator<RolloutEventKey> rolloutIterator = rolloutGroupEvents.iterator();
            if (!rolloutIterator.hasNext()) {
                return Optional.empty();
            }
            final RolloutEventKey eventKey = rolloutIterator.next();
            rolloutGroupEvents.clear();
            return Optional
                    .of(new RolloutGroupChangeEvent(eventKey.tenant, eventKey.rolloutId, eventKey.rolloutGroupId));
        }

        private int calculateQueueSize(final Optional<RolloutChangeEvent> rolloutChangeEvent,
                final Optional<RolloutGroupChangeEvent> rolloutGroupChangeEvent) {
            final AtomicInteger size = new AtomicInteger(queue.size());

            rolloutChangeEvent.ifPresent(event -> size.incrementAndGet());
            rolloutGroupChangeEvent.ifPresent(event -> size.incrementAndGet());

            return size.get();
        }

        /**
         * Checks if the tenant within the event is equal with the current
         * tenant in the context.
         *
         * @param userContext
         *            the security context of the current session
         * @param event
         *            the event to dispatch to the UI
         * @return {@code true} if the event can be dispatched to the UI
         *         otherwise {@code false}
         */
        private boolean eventSecurityCheck(final SecurityContext userContext,
                final org.eclipse.hawkbit.repository.event.TenantAwareEvent event) {
            if (userContext == null || userContext.getAuthentication() == null) {
                return false;
            }
            final Object tenantAuthenticationDetails = userContext.getAuthentication().getDetails();
            if (tenantAuthenticationDetails instanceof TenantAwareAuthenticationDetails) {
                return ((TenantAwareAuthenticationDetails) tenantAuthenticationDetails).getTenant()
                        .equalsIgnoreCase(event.getTenant());
            }
            return false;
        }

        private void doDispatch(final List<org.eclipse.hawkbit.repository.event.TenantAwareEvent> events,
                final WrappedSession wrappedSession) {
            final SecurityContext userContext = (SecurityContext) wrappedSession
                    .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            final SecurityContext oldContext = SecurityContextHolder.getContext();
            try {
                SecurityContextHolder.setContext(userContext);

                final List<EventContainer<TenantAwareEvent>> groupedEvents = groupEvents(events, userContext,
                        eventProvider);

                vaadinUI.access(() -> {
                    if (vaadinSession.getState() != State.OPEN) {
                        return;
                    }
                    LOG.debug("UI EventBus aggregator of UI {} got lock on session.", vaadinUI.getUIId());
                    groupedEvents.forEach(holder -> eventBus.publish(vaadinUI, holder));
                    LOG.debug("UI EventBus aggregator of UI {} left lock on session.", vaadinUI.getUIId());
                }).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("Wait for Vaadin session for UI {} interrupted!", vaadinUI.getUIId(), e);
            } finally {
                SecurityContextHolder.setContext(oldContext);
            }
        }

        @SuppressWarnings("unchecked")
        private List<EventContainer<TenantAwareEvent>> groupEvents(final List<TenantAwareEvent> events,
                final SecurityContext userContext, final UIEventProvider eventProvider) {

            return events.stream().filter(event -> eventSecurityCheck(userContext, event))
                    .collect(Collectors.groupingBy(TenantAwareEvent::getClass)).entrySet().stream().map(entry -> {
                        EventContainer<TenantAwareEvent> holder = null;
                        try {
                            final Constructor<TenantAwareEvent> declaredConstructor = (Constructor<TenantAwareEvent>) eventProvider
                                    .getEvents().get(entry.getKey()).getDeclaredConstructor(List.class);
                            declaredConstructor.setAccessible(true);

                            holder = (EventContainer<TenantAwareEvent>) declaredConstructor
                                    .newInstance(entry.getValue());
                        } catch (final ReflectiveOperationException e) {
                            LOG.error("Failed to create EventHolder!", e);
                        }

                        return holder;
                    }).collect(Collectors.toList());
        }
    }

    /**
     * An application event publisher subscriber which subscribes
     * {@link TenantAwareEvent} from the repository to dispatch these events to
     * the UI {@link SessionEventBus} .
     * 
     * @param applicationEvent
     *            the entity event which has been published from the repository
     */
    @Override
    @Async
    public void onApplicationEvent(final ApplicationEvent applicationEvent) {
        if (!(applicationEvent instanceof org.eclipse.hawkbit.repository.event.TenantAwareEvent)) {
            return;
        }

        final org.eclipse.hawkbit.repository.event.TenantAwareEvent event = (TenantAwareEvent) applicationEvent;

        collectRolloutEvent(event);
        // to dispatch too many events which are not interested on the UI
        if (!isEventProvided(event)) {
            LOG.trace("Event is not supported in the UI!!! Dropped event is {}", event);
            return;
        }

        if (!queue.offer(event)) {
            LOG.warn("Deque limit is reached, cannot add more events!!! Dropped event is {}", event);
            return;
        }
    }

    private static void collectRolloutEvent(final TenantAwareEvent event) {
        Long rolloutId = null;
        Long rolloutGroupId = null;
        if (event instanceof ActionCreatedEvent) {
            rolloutId = getRolloutId(((ActionCreatedEvent) event).getEntity().getRollout());
            rolloutGroupId = getRolloutGroupId(((ActionCreatedEvent) event).getEntity().getRolloutGroup());
        } else if (event instanceof ActionUpdatedEvent) {
            rolloutId = getRolloutId(((ActionUpdatedEvent) event).getEntity().getRollout());
            rolloutGroupId = getRolloutGroupId(((ActionUpdatedEvent) event).getEntity().getRolloutGroup());
        } else if (event instanceof RolloutUpdatedEvent) {
            rolloutId = ((RolloutUpdatedEvent) event).getEntityId();
        } else if (event instanceof RolloutGroupCreatedEvent) {
            rolloutId = ((RolloutGroupCreatedEvent) event).getRolloutId();
            rolloutGroupId = ((RolloutGroupCreatedEvent) event).getEntityId();
        } else if (event instanceof RolloutGroupUpdatedEvent) {
            final RolloutGroup rolloutGroup = ((RolloutGroupUpdatedEvent) event).getEntity();
            rolloutId = rolloutGroup.getRollout().getId();
            rolloutGroupId = rolloutGroup.getId();
        }

        if (rolloutId != null) {
            rolloutEvents.add(new RolloutEventKey(rolloutId, event.getTenant()));
            if (rolloutGroupId != null) {
                rolloutGroupEvents.add(new RolloutEventKey(rolloutId, rolloutGroupId, event.getTenant()));
            }
        }
    }

    private static Long getRolloutGroupId(final RolloutGroup rolloutGroup) {
        if (rolloutGroup != null) {
            return rolloutGroup.getId();
        }
        return null;
    }

    private static Long getRolloutId(final Rollout rollout) {
        if (rollout != null) {
            return rollout.getId();
        }
        return null;
    }

    /**
     * The rollout key in the concurrent set to be hold.
     * 
     *
     */
    private static final class RolloutEventKey {
        private final Long rolloutId;
        private final String tenant;
        private final Long rolloutGroupId;

        private RolloutEventKey(final Long rolloutId, final Long rolloutGroupId, final String tenant) {
            this.rolloutGroupId = rolloutGroupId;
            this.rolloutId = rolloutId;
            this.tenant = tenant;
        }

        private RolloutEventKey(final Long rolloutId, final String tenant) {
            this(rolloutId, null, tenant);
        }

        @Override
        public int hashCode() {// NOSONAR - as this is generated
            final int prime = 31;
            int result = 1;
            result = prime * result + ((rolloutGroupId == null) ? 0 : rolloutGroupId.hashCode());
            result = prime * result + ((rolloutId == null) ? 0 : rolloutId.hashCode());
            result = prime * result + ((tenant == null) ? 0 : tenant.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {// NOSONAR - as this is
                                                 // generated
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final RolloutEventKey other = (RolloutEventKey) obj;
            if (rolloutGroupId == null) {
                if (other.rolloutGroupId != null) {
                    return false;
                }
            } else if (!rolloutGroupId.equals(other.rolloutGroupId)) {
                return false;
            }
            if (rolloutId == null) {
                if (other.rolloutId != null) {
                    return false;
                }
            } else if (!rolloutId.equals(other.rolloutId)) {
                return false;
            }
            if (tenant == null) {
                if (other.tenant != null) {
                    return false;
                }
            } else if (!tenant.equals(other.tenant)) {
                return false;
            }
            return true;
        }

    }

}
