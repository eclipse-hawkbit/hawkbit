/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.error;

import org.eclipse.hawkbit.ui.menu.DashboardMenu;
import org.eclipse.hawkbit.ui.menu.DashboardMenuItem;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * View class that is instantiated when no other view matches the navigation
 * state.
 *
 * @see Navigator#setErrorView(Class)
 */
@SpringComponent
@UIScope
public class ErrorView extends VerticalLayout implements View {

    private static final long serialVersionUID = 1L;

    private final Label message;

    private final VaadinMessageSource i18n;

    private final DashboardMenu dashboardMenu;

    @Autowired
    ErrorView(final VaadinMessageSource i18n, final DashboardMenu dashboardMenu) {
        this.i18n = i18n;
        this.dashboardMenu = dashboardMenu;
        setMargin(true);
        setSpacing(false);

        message = new Label();
        addComponent(message);
    }

    @Override
    public void enter(final ViewChangeListener.ViewChangeEvent event) {
        final DashboardMenuItem view = dashboardMenu.getByViewName(event.getViewName());
        if (view == null) {
            message.setValue(i18n.getMessage("message.error.view", event.getViewName()));
            return;
        }
        if (dashboardMenu.isAccessDenied(event.getViewName())) {
            UINotification.showNotification(SPUIStyleDefinitions.SP_NOTIFICATION_ERROR_MESSAGE_STYLE,
                    i18n.getMessage("message.accessdenied"),
                    i18n.getMessage("message.accessdenied.view", event.getViewName()), null, true);
            message.setValue(i18n.getMessage("message.accessdenied.view", event.getViewName()));
        }
    }

}
