/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.push;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.repository.event.EntityEvent;
import org.eclipse.hawkbit.repository.event.Event;
import org.eclipse.hawkbit.ui.UIEventProvider;
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
    private final BlockingDeque<org.eclipse.hawkbit.repository.event.Event> queue = new LinkedBlockingDeque<>(
            BLOCK_SIZE);
    private int uiid = -1;

    @Autowired
    private ScheduledExecutorService executorService;

    @Autowired
    private EventBus.SessionEventBus eventBus;

    @Autowired
    private UIEventProvider eventProvider;
    private ScheduledFuture<?> jobHandle;

    private boolean isEventProvided(final org.eclipse.hawkbit.repository.event.Event event) {
        return eventProvider.getSingleEvents().contains(event.getClass())
                || eventProvider.getBulkEvents().contains(event.getClass());
    }

    @Override
    public void init(final UI vaadinUI) {
        uiid = vaadinUI.getUIId();
        LOG.info("Initialize delayed event push strategy for UI {}", uiid);
        if (vaadinUI.getSession() == null) {
            LOG.error("Vaadin session of UI {} is null! Event push disabled!", uiid);
        }

        jobHandle = executorService.scheduleWithFixedDelay(new DispatchRunnable(vaadinUI, vaadinUI.getSession()), 500,
                2000, TimeUnit.MILLISECONDS);
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

            final int size = queue.size();
            if (size <= 0) {
                LOG.debug("UI EventBus aggregator for UI {} has nothing to do.", vaadinUI.getUIId());
                return;
            }

            final List<Event> events = new ArrayList<>(size);
            final int eventsSize = queue.drainTo(events);

            if (events.isEmpty()) {
                LOG.debug("UI EventBus aggregator for UI {} has nothing to do.", vaadinUI.getUIId());
                return;
            }

            final WrappedSession wrappedSession = vaadinSession.getSession();
            if (wrappedSession == null) {
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
        private boolean eventSecurityCheck(final SecurityContext userContext,
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
                    LOG.debug("UI EventBus aggregator of UI {} got lock on session.", vaadinUI.getUIId());
                    fowardSingleEvents(events, userContext);
                    fowardBulkEvents(events, userContext);
                    LOG.debug("UI EventBus aggregator of UI {} left lock on session.", vaadinUI.getUIId());
                }).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Wait for Vaadin session for UI {} interrupted!", vaadinUI.getUIId(), e);
            } finally {
                SecurityContextHolder.setContext(oldContext);
            }
        }

        private void fowardBulkEvents(final List<Event> events, final SecurityContext userContext) {
            final Set<Class<?>> filterBulkEvenTypes = eventProvider.getFilteredBulkEventsType(events);

            for (final Class<?> bulkType : filterBulkEvenTypes) {
                final List<Event> listBulkEvents = events.stream()
                        .filter(event -> eventSecurityCheck(userContext, event) && bulkType.isInstance(event))
                        .collect(Collectors.toList());
                if (!listBulkEvents.isEmpty()) {
                    eventBus.publish(vaadinUI, listBulkEvents);
                }
            }
        }

        private void fowardSingleEvents(final List<Event> events, final SecurityContext userContext) {
            events.stream()
                    .filter(event -> eventSecurityCheck(userContext, event)
                            && eventProvider.getSingleEvents().contains(event.getClass()))
                    .forEach(event -> eventBus.publish(vaadinUI, event));
        }
    }

    /**
     * An application event publisher subscriber which subscribes
     * {@link EntityEvent} from the repository to dispatch these events to the
     * UI {@link SessionEventBus} .
     * 
     * @param applicationEvent
     *            the entity event which has been published from the repository
     */
    @Override
    @Async
    public void onApplicationEvent(final ApplicationEvent applicationEvent) {
        if (!(applicationEvent instanceof org.eclipse.hawkbit.repository.event.Event)) {
            return;
        }

        final org.eclipse.hawkbit.repository.event.Event event = (Event) applicationEvent;
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

}
