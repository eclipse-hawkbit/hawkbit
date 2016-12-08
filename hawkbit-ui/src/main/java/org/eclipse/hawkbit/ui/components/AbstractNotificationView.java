/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.components;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.event.TenantAwareEvent;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.common.table.BaseUIEntityEvent;
import org.eclipse.hawkbit.ui.menu.DashboardMenuItem;
import org.eclipse.hawkbit.ui.push.EventContainer;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.VerticalLayout;

/**
 * Abstract view for all views, which show notifications.
 */
public abstract class AbstractNotificationView extends VerticalLayout implements View {

    private static final long serialVersionUID = 1L;
    private final transient Cache<BaseUIEntityEvent<?>, Object> skipUiEvents = CacheBuilder.newBuilder()
            .expireAfterAccess(10, SECONDS).build();

    private final transient EventBus.UIEventBus eventBus;

    private transient Map<Class<?>, RefreshableContainer> supportedEvents;

    private final NotificationUnreadButton notificationUnreadButton;

    private final AtomicInteger viewUnreadNotifcations = new AtomicInteger(0);

    /**
     * Constructor.
     * 
     * @param eventBus
     *            the ui event bus
     * @param notificationUnreadButton
     *            the notificationUnreadButton
     */
    public AbstractNotificationView(final EventBus.UIEventBus eventBus,
            final NotificationUnreadButton notificationUnreadButton) {
        this.eventBus = eventBus;
        this.notificationUnreadButton = notificationUnreadButton;
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEventContainerEvent(final EventContainer<?> eventContainer) {
        if (!supportNotificationEventContainer(eventContainer.getClass()) || eventContainer.getEvents().isEmpty()) {
            return;
        }

        eventContainer.getEvents().stream().filter(event -> !anyEventMatch(event)).forEach(event -> {
            notificationUnreadButton.incrementUnreadNotification(this, eventContainer);
            viewUnreadNotifcations.incrementAndGet();
        });
        getDashboardMenuItem().setNotificationUnreadValue(viewUnreadNotifcations);
    }

    private boolean anyEventMatch(final TenantAwareEvent tenantAwareEvent) {
        return skipUiEvents.asMap().keySet().stream().anyMatch(uiEvent -> uiEvent.matchRemoteEvent(tenantAwareEvent));
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onUiEvent(final BaseUIEntityEvent<?> event) {
        if (BaseEntityEventType.ADD_ENTITY != event.getEventType()
                && BaseEntityEventType.REMOVE_ENTITIES != event.getEventType()
                && BaseEntityEventType.UPDATED_ENTITY != event.getEventType()) {
            return;
        }
        skipUiEvents.put(event, new Object());
    }

    @PostConstruct
    protected void subscribe() {
        eventBus.subscribe(this);
    }

    @PreDestroy
    protected void destroy() {
        eventBus.unsubscribe(this);
    }

    /**
     * Refresh the view by event container changes.
     * 
     * @param eventContainers
     *            event container which container changed
     * 
     */
    public void refreshView(final Set<Class<?>> eventContainers) {
        eventContainers.stream().filter(this::supportNotificationEventContainer).forEach(this::refreshContainer);
        clear();
    }

    private void refreshContainer(final Class<?> containerClazz) {
        getSupportedEvents().get(containerClazz).refreshContainer();
    }

    /**
     * Refresh the view by event container changes.
     * 
     * 
     */
    public void refreshView() {
        if (viewUnreadNotifcations.get() <= 0) {
            return;
        }
        getSupportedEvents().values().stream().forEach(container -> container.refreshContainer());
        clear();
    }

    private void clear() {
        viewUnreadNotifcations.set(0);
        getDashboardMenuItem().setNotificationUnreadValue(viewUnreadNotifcations);
    }

    private boolean supportNotificationEventContainer(final Class<?> eventContainerClass) {
        return getSupportedEvents().containsKey(eventContainerClass);
    }

    /**
     * @return the eventBus
     */
    public EventBus.UIEventBus getEventBus() {
        return eventBus;
    }

    /**
     * @return the supportedEvents
     */
    private Map<Class<?>, RefreshableContainer> getSupportedEvents() {
        if (supportedEvents == null) {
            supportedEvents = getSupportedPushEvents();
        }
        return supportedEvents;
    }

    @Override
    public void enter(final ViewChangeEvent event) {
        // intended to override
    }

    /**
     * @return the supportedEvents
     */
    protected abstract Map<Class<?>, RefreshableContainer> getSupportedPushEvents();

    protected abstract DashboardMenuItem getDashboardMenuItem();

}
