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

import org.eclipse.hawkbit.eventbus.event.EntityEvent;
import org.eclipse.hawkbit.eventbus.event.Event;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.ui.push.events.EventContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.SessionEventBus;

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
    private final ScheduledExecutorService executorService;
    private final BlockingDeque<Event> queue = new LinkedBlockingDeque<>(BLOCK_SIZE);
    private final EventBus.SessionEventBus eventBus;
    private final com.google.common.eventbus.EventBus systemEventBus;
    private int uiid = -1;

    private ScheduledFuture<?> jobHandle;

    private final UIEventProvider eventProvider;

    /**
     * Constructor.
     * 
     * @param executorService
     *            for scheduled execution of event forwarding to the UI
     * @param eventBus
     *            the session event bus to where the events should be dispatched
     * @param systemEventBus
     *            the system event bus where to retrieve the events from the
     *            back-end
     * @param eventProvider
     *            for event delegation to UI
     */
    public DelayedEventBusPushStrategy(final ScheduledExecutorService executorService, final SessionEventBus eventBus,
            final com.google.common.eventbus.EventBus systemEventBus, final UIEventProvider eventProvider) {
        this.executorService = executorService;
        this.eventBus = eventBus;
        this.systemEventBus = systemEventBus;
        this.eventProvider = eventProvider;
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
    public void dispatch(final Event event) {
        // to dispatch too many events which are not interested on the UI
        if (!isEventProvided(event)) {
            LOG.trace("Event is not supported in the UI!!! Dropped event is {}", event);
            return;
        }

        if (!queue.offer(event)) {
            LOG.trace("Deque limit is reached, cannot add more events for UI {}! Dropped event is {}", uiid, event);
            return;
        }
    }

    private boolean isEventProvided(final Event event) {
        return eventProvider.getEvents().containsKey(event.getClass());
    }

    @Override
    public void init(final UI vaadinUI) {
        uiid = vaadinUI.getUIId();
        LOG.info("Initialize delayed event push strategy for UI {}", uiid);
        if (vaadinUI.getSession() == null) {
            LOG.error("Vaadin session of UI {} is null! Event push disabled!", uiid);
        }

        jobHandle = executorService.scheduleWithFixedDelay(new DispatchRunnable(vaadinUI, vaadinUI.getSession()), 500,
                1_000, TimeUnit.MILLISECONDS);
        systemEventBus.register(this);
    }

    @Override
    public void clean() {
        LOG.info("Cleanup delayed event push strategy for UI", uiid);
        systemEventBus.unregister(this);
        jobHandle.cancel(true);
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
    protected static boolean eventSecurityCheck(final SecurityContext userContext, final Event event) {
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

        private void doDispatch(final List<Event> events, final WrappedSession wrappedSession) {
            final SecurityContext userContext = (SecurityContext) wrappedSession
                    .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            final SecurityContext oldContext = SecurityContextHolder.getContext();
            try {
                SecurityContextHolder.setContext(userContext);

                final List<EventContainer<Event>> groupedEvents = groupEvents(events, userContext, eventProvider);
                vaadinUI.access(() -> {
                    if (vaadinSession.getState() != State.OPEN) {
                        return;
                    }
                    LOG.debug("UI EventBus aggregator of UI {} got lock on session.", vaadinUI.getUIId());
                    groupedEvents.forEach(holder -> eventBus.publish(vaadinUI, holder));
                    LOG.debug("UI EventBus aggregator of UI {} left lock on session.", vaadinUI.getUIId());
                }).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Wait for Vaadin session for UI {} interrupted!", vaadinUI.getUIId(), e);
            } finally {
                SecurityContextHolder.setContext(oldContext);
            }
        }

        @SuppressWarnings("unchecked")
        private List<EventContainer<Event>> groupEvents(final List<Event> events, final SecurityContext userContext,
                final UIEventProvider eventProvider) {

            return events.stream().filter(event -> eventSecurityCheck(userContext, event))
                    .collect(Collectors.groupingBy(Event::getClass)).entrySet().stream().map(entry -> {
                        EventContainer<Event> holder = null;
                        try {
                            final Constructor<Event> declaredConstructor = (Constructor<Event>) eventProvider
                                    .getEvents().get(entry.getKey()).getDeclaredConstructor(List.class);
                            declaredConstructor.setAccessible(true);

                            holder = (EventContainer<Event>) declaredConstructor.newInstance(entry.getValue());
                        } catch (final ReflectiveOperationException e) {
                            LOG.error("Failed to create EventHolder!", e);
                        }

                        return holder;
                    }).collect(Collectors.toList());
        }
    }

}
