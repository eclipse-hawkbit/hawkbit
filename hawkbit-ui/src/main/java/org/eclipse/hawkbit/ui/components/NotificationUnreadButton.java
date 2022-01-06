/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.components;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EventNotificationType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Button which shows all notification in a popup.
 */
@SpringComponent
@UIScope
public class NotificationUnreadButton extends Button {
    private static final long serialVersionUID = 1L;

    private static final String TITLE = "notification.unread.button.title";
    private static final String DESCRIPTION = "notification.unread.button.description";

    private static final String STYLE = "notifications-unread";
    private static final String STYLE_UNREAD_COUNTER = "unread";
    private static final String STYLE_POPUP = "notifications-unread-popup";
    private static final String STYLE_NO_CLOSEBOX = "no-closebox";

    private final transient VaadinMessageSource i18n;
    private final transient UIEventBus eventBus;

    private int unreadNotificationCounter;
    private Window notificationsWindow;
    private final transient Map<EventNotificationType, EntityModifiedEventPayload> remotelyOriginatedEventsStore;

    /**
     * Constructor.
     * 
     * @param i18n
     *            i18n
     */
    @Autowired
    NotificationUnreadButton(final VaadinMessageSource i18n, final UIEventBus eventBus) {
        this.i18n = i18n;
        this.eventBus = eventBus;

        this.remotelyOriginatedEventsStore = new ConcurrentHashMap<>();

        setId(UIComponentIdProvider.NOTIFICATION_UNREAD_ID);
        setIcon(VaadinIcons.BELL);
        setCaptionAsHtml(true);
        setEnabled(false);
        addStyleName(ValoTheme.BUTTON_SMALL);
        addStyleName(STYLE);

        createNotificationWindow();
        addClickListener(this::dispatchRemotelyOriginatedEvents);
    }

    private void createNotificationWindow() {
        notificationsWindow = new Window();

        notificationsWindow.setId(UIComponentIdProvider.NOTIFICATION_UNREAD_POPUP_ID);
        notificationsWindow.setWidth(300.0F, Unit.PIXELS);
        notificationsWindow.setClosable(true);
        notificationsWindow.setResizable(false);
        notificationsWindow.setDraggable(false);
        notificationsWindow.addStyleName(STYLE_POPUP);
        notificationsWindow.addStyleName(STYLE_NO_CLOSEBOX);

        notificationsWindow.addCloseListener(event -> refreshCaption());
        notificationsWindow.addBlurListener(this::closeWindow);
    }

    private void refreshCaption() {
        setCaption(null);
        setEnabled(notificationsWindow.isAttached());
        if (unreadNotificationCounter > 0) {
            setVisible(true);
            setEnabled(true);
            setCaption("<div class='" + STYLE_UNREAD_COUNTER + "'>" + unreadNotificationCounter + "</div>");
        }
        setDescription(i18n.getMessage(DESCRIPTION, unreadNotificationCounter));
    }

    private void closeWindow(final BlurEvent event) {
        getUI().removeWindow((Window) event.getComponent());
    }

    private void dispatchRemotelyOriginatedEvents(final ClickEvent event) {
        if (notificationsWindow.isAttached()) {
            getUI().removeWindow(notificationsWindow);
            return;
        }

        notificationsWindow.setContent(buildNotificationsWindowLayout());
        notificationsWindow.setPositionY(event.getClientY() - event.getRelativeY() + 40);
        getUI().addWindow(notificationsWindow);

        dispatchEntityModifiedEvents();

        clear();
        notificationsWindow.focus();
    }

    private VerticalLayout buildNotificationsWindowLayout() {
        final VerticalLayout notificationsLayout = new VerticalLayout();
        notificationsLayout.setMargin(true);
        notificationsLayout.setSpacing(true);

        final Label title = new Label(i18n.getMessage(TITLE));
        title.addStyleName(ValoTheme.LABEL_H3);
        title.addStyleName(ValoTheme.LABEL_NO_MARGIN);

        notificationsLayout.addComponent(title);

        final Label[] eventNotificationLabels = remotelyOriginatedEventsStore.entrySet().stream()
                .map(this::buildEventNotificationLabel).toArray(Label[]::new);

        notificationsLayout.addComponents(eventNotificationLabels);

        return notificationsLayout;
    }

    private Label buildEventNotificationLabel(
            final Entry<EventNotificationType, EntityModifiedEventPayload> remotelyOriginatedEvent) {
        final EventNotificationType notificationType = remotelyOriginatedEvent.getKey();
        final int modifiedEntitiesCount = remotelyOriginatedEvent.getValue().getEntityIds().size();
        String message = "";
        if (modifiedEntitiesCount == 1) {
            message = i18n.getMessage(notificationType.getNotificationMessageKeySing());
        } else {
            message = i18n.getMessage(notificationType.getNotificationMessageKeyPlur(),
                    String.valueOf(modifiedEntitiesCount));
        }
        return new Label(message);
    }

    private void dispatchEntityModifiedEvents() {
        final Collection<EntityModifiedEventPayload> remotelyOriginatedEvents = remotelyOriginatedEventsStore.values();
        eventBus.publish(EventTopics.REMOTE_EVENT_DISPATCHED, UI.getCurrent(), remotelyOriginatedEvents);
        remotelyOriginatedEvents
                .forEach(eventPayload -> eventBus.publish(EventTopics.ENTITY_MODIFIED, UI.getCurrent(), eventPayload));
    }

    private void clear() {
        unreadNotificationCounter = 0;
        remotelyOriginatedEventsStore.clear();
        refreshCaption();
    }

    /**
     * Increments the unread notifications
     *
     * @param notificationType
     *            notification type for message
     * @param eventPayload
     *            EntityModifiedEventPayload
     */
    public void incrementUnreadNotification(final EventNotificationType notificationType,
            final EntityModifiedEventPayload eventPayload) {
        remotelyOriginatedEventsStore.merge(notificationType, eventPayload, (oldEventPayload, newEventPayload) -> {
            // currently we do not support parent aware differed events,
            // thus ignoring parentId of the incoming eventPayload
            oldEventPayload.getEntityIds().addAll(newEventPayload.getEntityIds());

            return oldEventPayload;
        });

        unreadNotificationCounter += eventPayload.getEntityIds().size();
        refreshCaption();
    }
}
