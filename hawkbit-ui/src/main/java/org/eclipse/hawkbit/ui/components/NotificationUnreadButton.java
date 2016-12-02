/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ui.components;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.hawkbit.ui.push.EventContainer;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.navigator.View;
import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
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

    private int unreadNotificationCounter;
    private AbstractNotificationView currentView;
    private Window notificationsWindow;
    private transient Map<Class<?>, NotificationUnreadValue> unreadNotifications;
    private transient I18N i18n;

    /**
     * Constructor.
     * 
     * @param i18n
     *            i18n
     */
    @Autowired
    public NotificationUnreadButton(final I18N i18n) {
        this.i18n = i18n;
        this.unreadNotifications = new ConcurrentHashMap<>();
        setIcon(FontAwesome.BELL);
        setId(UIComponentIdProvider.NOTIFICATION_UNREAD_ID);
        addStyleName(SPUIStyleDefinitions.ACTION_BUTTON);
        addStyleName(ValoTheme.BUTTON_SMALL);
        addStyleName(STYLE);
        setHtmlContentAllowed(true);
        createNotificationWindow();
        addClickListener(event -> toggleWindow(event));
    }

    private void createUnreadMessagesLayout() {
        final VerticalLayout notificationsLayout = new VerticalLayout();
        notificationsLayout.setMargin(true);
        notificationsLayout.setSpacing(true);

        final Label title = new Label(i18n.get(TITLE));
        title.addStyleName(ValoTheme.LABEL_H3);
        title.addStyleName(ValoTheme.LABEL_NO_MARGIN);
        notificationsLayout.addComponent(title);

        unreadNotifications.values().stream().forEach(value -> createNotification(notificationsLayout, value));
        notificationsWindow.setContent(notificationsLayout);
    }

    private void createNotificationWindow() {
        notificationsWindow = new Window();
        notificationsWindow.setWidth(300.0F, Unit.PIXELS);
        notificationsWindow.addStyleName(STYLE_POPUP);
        notificationsWindow.setClosable(true);
        notificationsWindow.setResizable(false);
        notificationsWindow.setDraggable(false);
    }

    private void toggleWindow(final ClickEvent event) {
        if (notificationsWindow.isAttached()) {
            notificationsWindow.close();
            return;
        }
        createUnreadMessagesLayout();
        notificationsWindow.setPositionY(event.getClientY() - event.getRelativeY() + 40);
        getUI().addWindow(notificationsWindow);
        notificationsWindow.focus();
        currentView.refreshView(unreadNotifications.keySet());
        clear();

    }

    private void createNotification(final VerticalLayout notificationsLayout,
            final NotificationUnreadValue notificationUnreadValue) {
        final Label contentLabel = new Label(notificationUnreadValue.getUnreadNotifications() + " "
                + i18n.get(notificationUnreadValue.getUnreadNotificationMessageKey()));
        notificationsLayout.addComponent(contentLabel);
    }

    public void setCurrentView(final View currentView) {
        if (!(currentView instanceof AbstractNotificationView)) {
            setEnabled(false);
            return;
        }
        setEnabled(true);
        this.currentView = (AbstractNotificationView) currentView;
        this.currentView.refreshView();
        clear();
    }

    private void clear() {
        unreadNotificationCounter = 0;
        unreadNotifications.clear();
        refreshCaption();
    }

    /**
     * 
     * @param view
     * @param newEventContainer
     */
    public void incrementUnreadNotification(final AbstractNotificationView view,
            final EventContainer<?> newEventContainer) {
        if (!currentView.equals(view) || newEventContainer.getUnreadNotificationMessageKey() == null) {
            return;
        }
        NotificationUnreadValue notificationUnreadValue = unreadNotifications.get(newEventContainer.getClass());
        if (notificationUnreadValue == null) {
            notificationUnreadValue = new NotificationUnreadValue(0,
                    newEventContainer.getUnreadNotificationMessageKey());
            unreadNotifications.put(newEventContainer.getClass(), notificationUnreadValue);
        }

        notificationUnreadValue.incrementUnreadNotifications();
        unreadNotificationCounter++;
        refreshCaption();
    }

    private void refreshCaption() {
        setCaption(null);
        if (unreadNotificationCounter > 0) {
            setVisible(true);
            setCaption("<div class='" + STYLE_UNREAD_COUNTER + "'>" + unreadNotificationCounter + "</div>");
        }
        setDescription(i18n.get(DESCRIPTION, new Object[] { unreadNotificationCounter }));
    }

    private static class NotificationUnreadValue {
        private Integer unreadNotifications;
        private final String unreadNotificationMessageKey;

        /**
         * @param unreadNotifications
         * @param unreadNotificationMessageKey
         */
        public NotificationUnreadValue(final Integer unreadNotifications, final String unreadNotificationMessageKey) {
            this.unreadNotifications = unreadNotifications;
            this.unreadNotificationMessageKey = unreadNotificationMessageKey;
        }

        /**
         * Increment the unread notifications.
         * 
         */
        public void incrementUnreadNotifications() {
            unreadNotifications++;
        }

        public String getUnreadNotificationMessageKey() {
            return unreadNotificationMessageKey;
        }

        public Integer getUnreadNotifications() {
            return unreadNotifications;
        }

    }
}
