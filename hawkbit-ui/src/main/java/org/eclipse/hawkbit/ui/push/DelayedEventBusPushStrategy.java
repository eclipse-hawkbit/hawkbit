/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.push;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.eventbus.event.DistributionSetTagCreatedBulkEvent;
import org.eclipse.hawkbit.eventbus.event.DistributionSetTagDeletedEvent;
import org.eclipse.hawkbit.eventbus.event.EntityEvent;
import org.eclipse.hawkbit.eventbus.event.RolloutChangeEvent;
import org.eclipse.hawkbit.eventbus.event.RolloutGroupChangeEvent;
import org.eclipse.hawkbit.eventbus.event.TargetCreatedEvent;
import org.eclipse.hawkbit.eventbus.event.TargetDeletedEvent;
import org.eclipse.hawkbit.eventbus.event.TargetInfoUpdateEvent;
import org.eclipse.hawkbit.eventbus.event.TargetTagCreatedBulkEvent;
import org.eclipse.hawkbit.eventbus.event.TargetTagDeletedEvent;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.SessionEventBus;

import com.google.common.collect.Sets;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.VaadinSession.State;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.UI;

/**
 * A {@link EventPushStrategy} implementation which retrieves events from
 * {@link com.google.common.eventbus.EventBus} and store them first in an queue
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
public class DelayedEventBusPushStrategy implements EventPushStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(DelayedEventBusPushStrategy.class);

    private static final int BLOCK_SIZE = 10_000;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final BlockingDeque<org.eclipse.hawkbit.eventbus.event.Event> queue = new LinkedBlockingDeque<>(BLOCK_SIZE);
    private final EventBus.SessionEventBus eventBus;
    private final com.google.common.eventbus.EventBus systemEventBus;

    private ScheduledFuture<?> jobHandle;

    /**
     * only events defined in the set are dispatched to the session event bus.
     */
    private static final Set<Class<?>> UI_EVENTS = Sets.newHashSet(TargetInfoUpdateEvent.class,
            TargetCreatedEvent.class, TargetDeletedEvent.class, RolloutChangeEvent.class, RolloutGroupChangeEvent.class,
            TargetTagCreatedBulkEvent.class, DistributionSetTagCreatedBulkEvent.class,TargetTagDeletedEvent.class,DistributionSetTagDeletedEvent.class);

    /**
     * Constructor.
     * 
     * @param eventBus
     *            the session event bus to where the events should be dispatched
     * @param systemEventBus
     *            the system event bus where to retrieve the events from the
     *            back-end
     */
    public DelayedEventBusPushStrategy(final SessionEventBus eventBus,
            final com.google.common.eventbus.EventBus systemEventBus) {
        this.eventBus = eventBus;
        this.systemEventBus = systemEventBus;
    }

    /**
     * An {@link com.google.common.eventbus.EventBus} subscriber which
     * subscribes {@link EntityEvent} from the repository to dispatch these
     * events to the UI {@link SessionEventBus}.
     *
     * @param event
     *            the entity event which has been published from the repository
     */
    @Subscribe
    @AllowConcurrentEvents
    public void dispatch(final org.eclipse.hawkbit.eventbus.event.Event event) {
        // to dispatch too many events which are not interested on the UI
        if (UI_EVENTS.contains(event.getClass()) && !queue.offer(event)) {
            LOG.warn("Deque limit is reached, cannot add more events!!! Dropped event is {}", event);
            return;
        }
    }

    @Override
    public void init(final UI vaadinUI) {
        LOG.debug("Initialize delayed event push strategy");
        jobHandle = executorService.scheduleWithFixedDelay(new DispatchRunnable(vaadinUI, vaadinUI.getSession()), 500,
                2000, TimeUnit.MILLISECONDS);
        systemEventBus.register(this);
    }

    @Override
    public void clean() {
        LOG.debug("Cleanup resources");
        jobHandle.cancel(true);
        systemEventBus.unregister(this);
        executorService.shutdownNow();
        queue.clear();
    }

    /**
     * Checks if the tenant within the event is equal with the current tenant in
     * the context.
     * 
     * @param userContext
     *            the security context of the current session
     * @param event
     *            the event to dispatch to the UI
     * @return {@code true} if the event can be dispatched to the UI otherwise
     *         {@code false}
     */
    protected boolean eventSecurityCheck(final SecurityContext userContext,
            final org.eclipse.hawkbit.eventbus.event.Event event) {
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

    private final class DispatchRunnable implements Runnable {

        private final UI vaadinUI;
        private final VaadinSession vaadinSession;

        private DispatchRunnable(final UI ui, final VaadinSession session) {
            vaadinUI = ui;
            vaadinSession = session;
        }

        @Override
        public void run() {
            LOG.debug("UI EventBus aggregator started");
            final long timestamp = System.currentTimeMillis();
            final List<org.eclipse.hawkbit.eventbus.event.Event> events = new LinkedList<>();
            for (int i = 0; i < BLOCK_SIZE; i++) {
                final org.eclipse.hawkbit.eventbus.event.Event pollEvent = queue.poll();
                if (pollEvent == null) {
                    continue;
                }
                events.add(pollEvent);
            }

            if (events.isEmpty()) {
                return;
            }

            if (vaadinSession == null) {
                return;
            }

            LOG.debug("UI EventBus aggregator session: {}", vaadinSession);

            final WrappedSession wrappedSession = vaadinSession.getSession();
            if (wrappedSession == null) {
                return;
            }

            final int eventsSize = events.size();

            doDispatch(events, wrappedSession);

            LOG.debug("UI EventBus aggregator done with sending {} events in {} ms", eventsSize,
                    System.currentTimeMillis() - timestamp);

        }

        private void doDispatch(final List<org.eclipse.hawkbit.eventbus.event.Event> events,
                final WrappedSession wrappedSession) {
            final SecurityContext userContext = (SecurityContext) wrappedSession
                    .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            final SecurityContext oldContext = SecurityContextHolder.getContext();
            try {
                SecurityContextHolder.setContext(userContext);
                vaadinUI.access(() -> {
                    if (vaadinSession.getState() != State.OPEN) {
                        return;
                    }
                    fowardEvents(events, userContext);

                    // send a list of events, because ui performance issues
                    publishEventAsList(events, userContext, TargetInfoUpdateEvent.class);
                    publishEventAsList(events, userContext, TargetCreatedEvent.class);
                    publishEventAsList(events, userContext, TargetDeletedEvent.class);
                });
            } finally {
                SecurityContextHolder.setContext(oldContext);
            }
        }

        private void publishEventAsList(final List<org.eclipse.hawkbit.eventbus.event.Event> events,
                final SecurityContext userContext, final Class<?> eventType) {
            final List<org.eclipse.hawkbit.eventbus.event.Event> bulkEvents = events.stream()
                    .filter(event -> DelayedEventBusPushStrategy.this.eventSecurityCheck(userContext, event)
                            && eventType.isInstance(event))
                    .collect(Collectors.toList());
            if (bulkEvents.isEmpty()) {
                return;
            }
            eventBus.publish(vaadinUI, bulkEvents);
        }

        private void fowardEvents(final List<org.eclipse.hawkbit.eventbus.event.Event> events,
                final SecurityContext userContext) {
            events.stream().filter(event -> DelayedEventBusPushStrategy.this.eventSecurityCheck(userContext, event))
                    .forEach(event -> eventBus.publish(vaadinUI, event));
        }
    }

}
