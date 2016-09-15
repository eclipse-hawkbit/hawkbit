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

import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.repository.event.EntityEvent;
import org.eclipse.hawkbit.ui.UIEventProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
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
    private final BlockingDeque<org.eclipse.hawkbit.repository.event.Event> queue = new LinkedBlockingDeque<>(
            BLOCK_SIZE);

    private EventBus.SessionEventBus eventBus;

    @Autowired
    private UIEventProvider eventProvider;
    private ScheduledFuture<?> jobHandle;

    /**
     * Upstream, based on branch 'Feature_Horizontal_Scalability' of
     * https://github.com/bsinno/hawkbit.git An application event publisher
     * subscriber which subscribes {@link EntityEvent} from the repository to
     * dispatch these events to the UI {@link SessionEventBus} .
     * 
     * @param event
     *            the entity event which has been published from the repository
     */
    @EventListener(classes = org.eclipse.hawkbit.repository.event.Event.class)
    public void dispatch(final org.eclipse.hawkbit.repository.event.Event event) {

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

    private boolean isEventProvided(final org.eclipse.hawkbit.repository.event.Event event) {
        return eventProvider.getSingleEvents().contains(event.getClass())
                || eventProvider.getBulkEvents().contains(event.getClass());
    }

    @Override
    public void init(final UI vaadinUI) {
        LOG.debug("Initialize delayed event push strategy");
        jobHandle = executorService.scheduleWithFixedDelay(new DispatchRunnable(vaadinUI, vaadinUI.getSession()), 500,
                2000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void clean() {
        LOG.debug("Cleanup resources");
        jobHandle.cancel(true);
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
            final org.eclipse.hawkbit.repository.event.Event event) {
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
            final List<org.eclipse.hawkbit.repository.event.Event> events = new LinkedList<>();
            for (int i = 0; i < BLOCK_SIZE; i++) {
                final org.eclipse.hawkbit.repository.event.Event pollEvent = queue.poll();
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

        private void doDispatch(final List<org.eclipse.hawkbit.repository.event.Event> events,
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
                    fowardSingleEvents(events, userContext);
                    fowardBulkEvents(events, userContext);
                });
            } finally {
                SecurityContextHolder.setContext(oldContext);
            }
        }

        private void fowardBulkEvents(final List<org.eclipse.hawkbit.repository.event.Event> events,
                final SecurityContext userContext) {
            final Set<Class<?>> filterBulkEvenTypes = eventProvider.getFilteredBulkEventsType(events);
            publishBulkEvent(events, userContext, filterBulkEvenTypes);
        }

        private void publishBulkEvent(final List<org.eclipse.hawkbit.repository.event.Event> events,
                final SecurityContext userContext, final Set<Class<?>> filterBulkEvenTypes) {
            for (final Class<?> bulkType : filterBulkEvenTypes) {
                final List<org.eclipse.hawkbit.repository.event.Event> listBulkEvents = events.stream()
                        .filter(event -> DelayedEventBusPushStrategy.this.eventSecurityCheck(userContext, event)
                                && bulkType.isInstance(event))
                        .collect(Collectors.toList());
                if (!listBulkEvents.isEmpty()) {
                    eventBus.publish(vaadinUI, listBulkEvents);
                }
            }
        }

        private void fowardSingleEvents(final List<org.eclipse.hawkbit.repository.event.Event> events,
                final SecurityContext userContext) {
            events.stream()
                    .filter(event -> DelayedEventBusPushStrategy.this.eventSecurityCheck(userContext, event)
                            && eventProvider.getSingleEvents().contains(event.getClass()))
                    .forEach(event -> eventBus.publish(vaadinUI, event));
        }
    }

    /**
     * @param eventBus
     *            the eventBus to set
     */
    @Override
    public void setEventBus(final EventBus.SessionEventBus eventBus) {
        this.eventBus = eventBus;
    }

}
