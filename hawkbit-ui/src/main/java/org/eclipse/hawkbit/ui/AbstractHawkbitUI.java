/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui;

import org.eclipse.hawkbit.ui.components.NotificationUnreadButton;
import org.eclipse.hawkbit.ui.error.ErrorView;
import org.eclipse.hawkbit.ui.menu.DashboardEvent.PostViewChangeEvent;
import org.eclipse.hawkbit.ui.menu.DashboardMenu;
import org.eclipse.hawkbit.ui.menu.DashboardMenuItem;
import org.eclipse.hawkbit.ui.push.EventPushStrategy;
import org.eclipse.hawkbit.ui.push.UIEventProvider;
import org.eclipse.hawkbit.ui.themes.HawkbitTheme;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.AbstractApplicationContext;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.server.ClientConnector.DetachListener;
import com.vaadin.server.ErrorHandler;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.navigator.SpringViewProvider;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Vaadin management UI.
 *
 */
@Title("hawkBit Update Server")
@Widgetset(value = HawkbitTheme.WIDGET_SET_NAME)
@Theme(HawkbitTheme.THEME_NAME)
public abstract class AbstractHawkbitUI extends UI implements DetachListener {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractHawkbitUI.class);

    private static final String EMPTY_VIEW = "";

    private final VaadinMessageSource i18n;
    private final UiProperties uiProperties;

    private Label viewTitle;

    private final DashboardMenu dashboardMenu;
    private final ErrorView errorview;
    private final NotificationUnreadButton notificationUnreadButton;

    private final SpringViewProvider viewProvider;
    private final transient ApplicationContext context;
    private final transient EventPushStrategy pushStrategy;
    private final transient ErrorHandler uiErrorHandler;

    private final transient HawkbitEntityEventListener entityEventsListener;

    protected AbstractHawkbitUI(final EventPushStrategy pushStrategy, final UIEventBus eventBus,
            final UIEventProvider eventProvider, final SpringViewProvider viewProvider,
            final ApplicationContext context, final DashboardMenu dashboardMenu, final ErrorView errorview,
            final NotificationUnreadButton notificationUnreadButton, final UiProperties uiProperties,
            final VaadinMessageSource i18n, final ErrorHandler uiErrorHandler) {
        this.pushStrategy = pushStrategy;
        this.viewProvider = viewProvider;
        this.context = context;
        this.dashboardMenu = dashboardMenu;
        this.errorview = errorview;
        this.notificationUnreadButton = notificationUnreadButton;
        this.uiProperties = uiProperties;
        this.i18n = i18n;
        this.uiErrorHandler = uiErrorHandler;

        this.entityEventsListener = new HawkbitEntityEventListener(eventBus, eventProvider, notificationUnreadButton);
    }

    @Override
    public void detach(final DetachEvent event) {
        LOG.debug("ManagementUI is detached uiid - {}", getUIId());

        entityEventsListener.unsubscribeListeners();

        if (pushStrategy != null) {
            pushStrategy.clean();
            clearContextListener();
        }
    }

    private void clearContextListener() {
        if (pushStrategy instanceof ApplicationListener && context instanceof AbstractApplicationContext) {
            final ApplicationListener<?> listener = (ApplicationListener<?>) pushStrategy;
            ((AbstractApplicationContext) context).getApplicationListeners().remove(listener);

            // we do not need to explicitly remove the listener from
            // ApplicationEventMulticaster because it is done by
            // UIBeanStore#destroy delegating to
            // ApplicationListenerDetector#postProcessBeforeDestruction
        }
    }

    @Override
    protected void init(final VaadinRequest vaadinRequest) {
        LOG.debug("ManagementUI init starts uiid - {}", getUI().getUIId());
        if (pushStrategy != null) {
            pushStrategy.init(getUI());
        }
        addDetachListener(this);

        Responsive.makeResponsive(this);
        addStyleName(ValoTheme.UI_WITH_MENU);
        setResponsive(Boolean.TRUE);

        final HorizontalLayout rootLayout = new HorizontalLayout();
        rootLayout.setMargin(false);
        rootLayout.setSpacing(false);
        rootLayout.setSizeFull();

        HawkbitCommonUtil.initLocalization(this, uiProperties.getLocalization(), i18n);
        SPDateTimeUtil.initializeFixedTimeZoneProperty(uiProperties.getFixedTimeZone());

        dashboardMenu.init();
        dashboardMenu.setResponsive(true);

        final VerticalLayout contentVerticalLayout = new VerticalLayout();
        contentVerticalLayout.setMargin(false);
        contentVerticalLayout.setSpacing(false);
        contentVerticalLayout.setSizeFull();
        contentVerticalLayout.setStyleName("main-content");
        contentVerticalLayout.addComponent(buildHeader());
        contentVerticalLayout.addComponent(buildViewTitle());

        final Panel content = buildContent();
        contentVerticalLayout.addComponent(content);
        contentVerticalLayout.setExpandRatio(content, 1.0F);

        rootLayout.addComponent(dashboardMenu);
        rootLayout.addComponent(contentVerticalLayout);
        rootLayout.setExpandRatio(contentVerticalLayout, 1.0F);
        setContent(rootLayout);

        final Navigator navigator = new Navigator(this, content);
        navigator.addViewChangeListener(new ViewChangeListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean beforeViewChange(final ViewChangeEvent event) {
                return true;
            }

            @Override
            public void afterViewChange(final ViewChangeEvent event) {
                final DashboardMenuItem view = dashboardMenu.getByViewName(event.getViewName());
                dashboardMenu.postViewChange(new PostViewChangeEvent(view));
                if (view == null) {
                    viewTitle.setCaption(null);
                    return;
                }
                viewTitle.setCaption(view.getDashboardCaptionLong());
            }
        });

        navigator.setErrorView(errorview);
        navigator.addView(EMPTY_VIEW, new Navigator.EmptyView());
        navigator.addProvider(new ManagementViewProvider());
        setNavigator(navigator);

        if (UI.getCurrent().getErrorHandler() == null) {
            UI.getCurrent().setErrorHandler(uiErrorHandler);
        }

        LOG.debug("Current locale of the application is : {}", getLocale());
    }

    private static Panel buildContent() {
        final Panel content = new Panel();
        content.setSizeFull();
        content.setStyleName("view-content");
        return content;
    }

    private HorizontalLayout buildViewTitle() {
        final HorizontalLayout viewHeadercontent = new HorizontalLayout();
        viewHeadercontent.setMargin(false);
        viewHeadercontent.setSpacing(false);
        viewHeadercontent.setWidth("100%");
        viewHeadercontent.addStyleName("view-header-layout");

        viewTitle = new Label();
        viewTitle.setWidth("100%");
        viewTitle.setStyleName("header-content");
        viewHeadercontent.addComponent(viewTitle);

        viewHeadercontent.addComponent(notificationUnreadButton);
        viewHeadercontent.setComponentAlignment(notificationUnreadButton, Alignment.MIDDLE_RIGHT);
        return viewHeadercontent;
    }

    private static Component buildHeader() {
        final CssLayout cssLayout = new CssLayout();
        cssLayout.setStyleName("view-header");
        return cssLayout;
    }

    private class ManagementViewProvider implements ViewProvider {
        private static final long serialVersionUID = 1L;

        private static final String DEFAULT_PARAMETER_SEPARATOR = "/";

        @Override
        public String getViewName(final String viewAndParameters) {
            final int paramsDelimeterIndex = viewAndParameters.indexOf(DEFAULT_PARAMETER_SEPARATOR);
            final String viewName = paramsDelimeterIndex != -1 ? viewAndParameters.substring(0, paramsDelimeterIndex)
                    : viewAndParameters;
            return viewProvider.getViewName(getStartView(viewName));
        }

        @Override
        public View getView(final String viewName) {
            return viewProvider.getView(viewName);
        }

        private String getStartView(final String viewName) {
            final DashboardMenuItem view = dashboardMenu.getByViewName(viewName);
            if ("".equals(viewName) && !dashboardMenu.isAccessibleViewsEmpty()) {
                return dashboardMenu.getInitialViewName();
            }
            if (view == null || dashboardMenu.isAccessDenied(viewName)) {
                return " ";
            }
            return viewName;
        }
    }
}
