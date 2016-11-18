/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ui.components;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.ui.common.table.AbstractTable;
import org.eclipse.hawkbit.ui.push.event.NotificationEntityChangeEvent;
import org.eclipse.hawkbit.ui.push.event.RemoveNotificationEvent;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.google.common.collect.Sets;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 *
 */
@SpringComponent
@UIScope
public class NotificationUnreadButton extends Button {

    private static final long serialVersionUID = 1L;
    private static final String STYLE = "notifications-unread";
    private static final String STYLE_POPUP = "notifications-unread-popup";
    private static final String STYLE_UNREAD_COUNTER = "change-counter";
    private final Map<AbstractTable<?, ?>, Set<NotificationEntityChangeEvent>> notificationChangeEvents = new ConcurrentHashMap<>();

    private Window notificationsWindow;
    private int unreadNotficationCounter;

    @Autowired
    protected transient EventBus.SessionEventBus eventBus;

    public NotificationUnreadButton() {
        setIcon(FontAwesome.BELL);
        setId(UIComponentIdProvider.NOTIFICATION_UNREAD_ID);
        addStyleName(STYLE);
        setCaptionAsHtml(true);
    }

    @PostConstruct
    protected void init() {
        eventBus.subscribe(this);
        addClickListener(event -> openUnreadMessages(event));
    }

    @PreDestroy
    protected void destroy() {
        eventBus.unsubscribe(this);
    }

    private void updateUnreadNotificationLabel() {

        String description = "Notifications";
        if (unreadNotficationCounter > 0) {
            setVisible(true);
            setCaption("<div class='" + STYLE_UNREAD_COUNTER + "'>" + unreadNotficationCounter + "</div>");
            description += " (" + unreadNotficationCounter + " unread)";
        } else {
            setCaption(null);
        }
        setDescription(description);
    }

    private void openUnreadMessages(final ClickEvent event) {
        final VerticalLayout notificationsLayout = new VerticalLayout();
        notificationsLayout.setMargin(true);
        notificationsLayout.setSpacing(true);

        final Label title = new Label("Notifications");
        title.addStyleName(ValoTheme.LABEL_H3);
        title.addStyleName(ValoTheme.LABEL_NO_MARGIN);
        notificationsLayout.addComponent(title);

        notificationChangeEvents.values().stream().flatMap(notificationChangeEvent -> notificationChangeEvent.stream())
                .forEach(notificationChangeEvent -> createNotfication(notificationsLayout, notificationChangeEvent));

        final HorizontalLayout footer = new HorizontalLayout();
        footer.addStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR);
        footer.setWidth("100%");
        final Button refreshAll = new Button("Refresh All");
        refreshAll.addClickListener(listener -> {
            notificationChangeEvents.keySet().stream().forEach(AbstractTable::refreshContainer);
            clear();
            notificationsWindow.close();
        });

        refreshAll.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
        refreshAll.addStyleName(ValoTheme.BUTTON_SMALL);
        footer.addComponent(refreshAll);
        footer.setComponentAlignment(refreshAll, Alignment.TOP_CENTER);
        notificationsLayout.addComponent(footer);

        createNotficationWindow(notificationsLayout);

        if (notificationsWindow.isAttached()) {
            notificationsWindow.close();
        } else {
            notificationsWindow.setPositionY(event.getClientY() - event.getRelativeY() + 40);
            getUI().addWindow(notificationsWindow);
            notificationsWindow.focus();
        }

    }

    public void clear() {
        notificationChangeEvents.clear();
        unreadNotficationCounter = 0;
        updateUnreadNotificationLabel();
    }

    private void createNotficationWindow(final VerticalLayout notificationsLayout) {
        if (notificationsWindow != null) {
            notificationsWindow.setContent(notificationsLayout);
            return;
        }
        notificationsWindow = new Window();
        notificationsWindow.setWidth(300.0F, Unit.PIXELS);
        notificationsWindow.addStyleName(STYLE_POPUP);
        notificationsWindow.setClosable(false);
        notificationsWindow.setResizable(false);
        notificationsWindow.setDraggable(false);
        notificationsWindow.addCloseShortcut(KeyCode.ESCAPE, null);
        notificationsWindow.setContent(notificationsLayout);
    }

    private static void createNotfication(final VerticalLayout notificationsLayout,
            final NotificationEntityChangeEvent notification) {
        final Label contentLabel = new Label(
                notification.getUnreadNotificationSize() + " " + notification.getMessage());
        notificationsLayout.addComponent(contentLabel);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    protected void updateNotificationsCount(final NotificationEntityChangeEvent event) {
        unreadNotficationCounter += event.getUnreadNotificationSize();
        updateUnreadNotificationLabel();

        final Set<NotificationEntityChangeEvent> oldNotificationChangeEvents = notificationChangeEvents
                .get(event.getSender());

        if (oldNotificationChangeEvents == null) {
            notificationChangeEvents.put(event.getSender(), Sets.newHashSet(event));
            return;
        }

        if (!oldNotificationChangeEvents.contains(event)) {
            oldNotificationChangeEvents.add(event);
            return;
        }

        oldNotificationChangeEvents.stream().filter(oldEvent -> oldEvent.equals(event)).forEach(oldEvent -> oldEvent
                .setUnreadNotificationSize(oldEvent.getUnreadNotificationSize() + event.getUnreadNotificationSize()));

    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    protected void removeNotfication(final RemoveNotificationEvent event) {
        final Set<NotificationEntityChangeEvent> oldNotificationChangeEvents = notificationChangeEvents
                .get(event.getSender());

        if (oldNotificationChangeEvents != null) {
            oldNotificationChangeEvents.stream()
                    .forEach(oldEvent -> unreadNotficationCounter -= oldEvent.getUnreadNotificationSize());
        }
        updateUnreadNotificationLabel();
        notificationChangeEvents.remove(event.getSender());
    }

}
