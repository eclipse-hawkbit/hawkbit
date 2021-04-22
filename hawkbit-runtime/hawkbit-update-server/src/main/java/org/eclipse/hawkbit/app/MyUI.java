/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.app;

import org.eclipse.hawkbit.ui.AbstractHawkbitUI;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.components.NotificationUnreadButton;
import org.eclipse.hawkbit.ui.error.ErrorView;
import org.eclipse.hawkbit.ui.menu.DashboardMenu;
import org.eclipse.hawkbit.ui.push.EventPushStrategy;
import org.eclipse.hawkbit.ui.push.UIEventProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.annotations.Push;
import com.vaadin.server.ErrorHandler;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.navigator.SpringViewProvider;

/**
 * Example hawkBit UI implementation.
 * 
 * A {@link SpringUI} annotated class must be present in the classpath. The
 * easiest way to get an hawkBit UI running is to extend the
 * {@link AbstractHawkbitUI} and to annotated it with {@link SpringUI} as in
 * this example. WEBSOCKET_XHR transport is used instead of WEBSOCKET in order
 * to preserve Spring Security Context, that does not work using websocket
 * communication with Vaadin Shared Security.
 *
 */
@SpringUI
@Push(value = PushMode.AUTOMATIC, transport = Transport.WEBSOCKET_XHR)
// Exception squid:MaximumInheritanceDepth - Most of the inheritance comes from
// Vaadin.
@SuppressWarnings({ "squid:MaximumInheritanceDepth" })
public class MyUI extends AbstractHawkbitUI {
    private static final long serialVersionUID = 1L;

    @Autowired
    MyUI(final EventPushStrategy pushStrategy, final UIEventBus eventBus, final UIEventProvider eventProvider,
            final SpringViewProvider viewProvider, final ApplicationContext context, final DashboardMenu dashboardMenu,
            final ErrorView errorview, final NotificationUnreadButton notificationUnreadButton,
            final UiProperties uiProperties, final VaadinMessageSource i18n, final ErrorHandler uiErrorHandler) {
        super(pushStrategy, eventBus, eventProvider, viewProvider, context, dashboardMenu, errorview,
                notificationUnreadButton, uiProperties, i18n, uiErrorHandler);
    }

}
