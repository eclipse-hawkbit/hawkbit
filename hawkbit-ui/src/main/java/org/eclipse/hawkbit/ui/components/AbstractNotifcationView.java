/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ui.components;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Map;
import java.util.Set;

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
public abstract class AbstractNotifcationView extends VerticalLayout implements View {

    private static final long serialVersionUID = 1L;
    private final transient Cache<BaseUIEntityEvent<?>, Object> skipUiEvents = CacheBuilder.newBuilder()
            .expireAfterAccess(10, SECONDS).build();

    private final transient EventBus.UIEventBus eventBus;

    private transient Map<Class<?>, RefreshableContainer> supportedEvents;

    private final NotificationUnreadButton notificationUnreadButton;

    private int viewUnreadNotifcations;

    /**
     * Constructor.
     * 
     * @param eventBus
     *            the ui event bus
     * @param notificationUnreadButton
     *            the notificationUnreadButton
     */
    public AbstractNotifcationView(final EventBus.UIEventBus eventBus,
            final NotificationUnreadButton notificationUnreadButton) {
        this.eventBus = eventBus;
        this.notificationUnreadButton = notificationUnreadButton;
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEventContainerEvent(final EventContainer<?> eventContainer) {
        if (!supportNotifcationEventContainer(eventContainer.getClass()) || eventContainer.getEvents().isEmpty()) {
            return;
        }

        eventContainer.getEvents().stream().filter(event -> !anyEventMatch(event)).forEach(event -> {
            notificationUnreadButton.incrementUnreadNotification(this, eventContainer);
            viewUnreadNotifcations++;
        });
        getDashboardMenuItem().setNotificationUnreadValue(viewUnreadNotifcations);
    }

    private boolean anyEventMatch(final TenantAwareEvent tenantAwareEvent) {
        return skipUiEvents.asMap().keySet().stream().anyMatch(uiEvent -> uiEvent.matchRemoteEvent(tenantAwareEvent));
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onUiEvent(final BaseUIEntityEvent<?> event) {
        if (BaseEntityEventType.ADD_ENTITY != event.getEventType()
                && BaseEntityEventType.REMOVE_ENTITIES != event.getEventType()) {
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
        eventContainers.stream().filter(clazz -> supportNotifcationEventContainer(clazz))
                .forEach(clazz -> getSupportedEvents().get(clazz).refreshContainer());
        clear();
    }

    /**
     * Refresh the view by event container changes.
     * 
     * 
     */
    public void refreshView() {
        if (viewUnreadNotifcations <= 0) {
            return;
        }
        supportedEvents.values().stream().forEach(container -> container.refreshContainer());
        clear();
    }

    private void clear() {
        viewUnreadNotifcations = 0;
        getDashboardMenuItem().setNotificationUnreadValue(viewUnreadNotifcations);
    }

    protected boolean supportNotifcationEventContainer(final Class<?> eventContainerClass) {
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
            supportedEvents = getSupportedViewEvents();
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
    protected abstract Map<Class<?>, RefreshableContainer> getSupportedViewEvents();

    protected abstract DashboardMenuItem getDashboardMenuItem();

}
