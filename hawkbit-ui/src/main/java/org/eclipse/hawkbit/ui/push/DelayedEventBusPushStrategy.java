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
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.repository.event.TenantAwareEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutUpdatedEvent;
import org.eclipse.hawkbit.ui.push.event.RolloutChangedEvent;
import org.eclipse.hawkbit.ui.push.event.RolloutGroupChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.SessionEventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.server.VaadinSession;
import com.vaadin.server.VaadinSession.State;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.UI;

/**
 * An {@link EventPushStrategy} implementation which retrieves events from
 * {@link com.google.common.eventbus.EventBus} and store them first in a queue
 * where they will dispatched every x (default is 2 and can be configured with
 * the property) seconds to the {@link EventBus} in a Vaadin access thread
 * {@link UI#access(Runnable)}.
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
    private final BlockingDeque<TenantAwareEvent> queue = new LinkedBlockingDeque<>(BLOCK_SIZE);

    private final ScheduledExecutorService executorService;
    private final EventBus.UIEventBus eventBus;
    private final UIEventProvider eventProvider;
    private final long delay;

    private ScheduledFuture<?> jobHandle;
    private UI vaadinUI;

    /**
     * Constructor.
     * 
     * @param executorService
     *            the general scheduler service
     * @param eventBus
     *            the ui event bus
     * @param eventProvider
     *            the event provider
     * @param delay
     *            the delay for the event forwarding. Every delay millisecond
     *            the events are forwarded by this strategy
     */
    public DelayedEventBusPushStrategy(final ScheduledExecutorService executorService, final UIEventBus eventBus,
            final UIEventProvider eventProvider, final long delay) {
        this.executorService = executorService;
        this.eventBus = eventBus;
        this.eventProvider = eventProvider;
        this.delay = delay;
    }

    private boolean isEventProvided(final TenantAwareEvent event) {
        return eventProvider.getEvents().containsKey(event.getClass());
    }

    @Override
    public void init(final UI vaadinUI) {
        this.vaadinUI = vaadinUI;
        LOG.info("Initialize delayed event push strategy for UI {}", vaadinUI.getUIId());
        if (vaadinUI.getSession() == null) {
            LOG.error("Vaadin session of UI {} is null! Event push disabled!", vaadinUI.getUIId());
        }

        jobHandle = executorService.scheduleWithFixedDelay(new DispatchRunnable(vaadinUI, vaadinUI.getSession()),
                10_000, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void clean() {
        LOG.info("Cleanup delayed event push strategy for UI", vaadinUI.getUIId());
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

            final int size = queue.size();
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
        private boolean eventSecurityCheck(final SecurityContext userContext, final TenantAwareEvent event) {
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

        private void doDispatch(final List<TenantAwareEvent> events, final WrappedSession wrappedSession) {
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
    public void onApplicationEvent(final ApplicationEvent applicationEvent) {
        if (!(applicationEvent instanceof TenantAwareEvent)) {
            return;
        }

        final TenantAwareEvent event = (TenantAwareEvent) applicationEvent;

        collectRolloutEvent(event);
        // to dispatch too many events which are not interested on the UI
        if (!isEventProvided(event)) {
            LOG.trace("Event is not supported in the UI!!! Dropped event is {}", event);
            return;
        }
        offerEvent(event);
    }

    private void offerEventIfNotContains(final TenantAwareEvent event) {
        if (queue.contains(event)) {
            return;
        }
        offerEvent(event);
    }

    private void offerEvent(final TenantAwareEvent event) {
        if (!queue.offer(event)) {
            LOG.trace("Deque limit is reached, cannot add more events!!! Dropped event is {}", event);
        }
    }

    private void collectRolloutEvent(final TenantAwareEvent event) {
        Long rolloutId;
        Long rolloutGroupId = null;
        if (event instanceof ActionCreatedEvent) {
            rolloutId = ((ActionCreatedEvent) event).getRolloutId();
            rolloutGroupId = ((ActionCreatedEvent) event).getRolloutGroupId();
        } else if (event instanceof ActionUpdatedEvent) {
            rolloutId = ((ActionUpdatedEvent) event).getRolloutId();
            rolloutGroupId = ((ActionUpdatedEvent) event).getRolloutGroupId();
        } else if (event instanceof RolloutUpdatedEvent) {
            rolloutId = ((RolloutUpdatedEvent) event).getEntityId();
        } else if (event instanceof RolloutGroupCreatedEvent) {
            rolloutId = ((RolloutGroupCreatedEvent) event).getRolloutId();
            rolloutGroupId = ((RolloutGroupCreatedEvent) event).getEntityId();
        } else if (event instanceof RolloutGroupUpdatedEvent) {
            rolloutId = ((RolloutGroupUpdatedEvent) event).getRolloutId();
            rolloutGroupId = ((RolloutGroupUpdatedEvent) event).getEntityId();
        } else {
            return;
        }

        offerEventIfNotContains(new RolloutChangedEvent(event.getTenant(), rolloutId));

        if (rolloutGroupId != null) {
            offerEventIfNotContains(new RolloutGroupChangedEvent(event.getTenant(), rolloutId, rolloutGroupId));
        }
    }
}
