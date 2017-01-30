/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.login;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.hawkbit.ui.DefaultHawkbitUI;
import org.eclipse.hawkbit.ui.HawkbitUI;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import com.vaadin.annotations.Title;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.navigator.SpringViewProvider;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

/**
 * Login UI window that is independent of the {@link HawkbitUI} itself.
 *
 */
@Title("hawkBit UI - Login")
public class HawkbitLoginUI extends DefaultHawkbitUI {
    private static final Logger LOG = LoggerFactory.getLogger(HawkbitLoginUI.class);

    private static final long serialVersionUID = 1L;

    private final SpringViewProvider viewProvider;

    private final transient ApplicationContext context;

    @Autowired
    protected HawkbitLoginUI(final SpringViewProvider viewProvider, final ApplicationContext context) {
        this.viewProvider = viewProvider;
        this.context = context;
    }

    @Override
    protected void init(final VaadinRequest request) {
        SpringContextHelper.setContext(context);

        final VerticalLayout rootLayout = new VerticalLayout();
        final Component header = buildHeader();

        rootLayout.addComponent(header);
        rootLayout.setSizeFull();

        final HorizontalLayout content = new HorizontalLayout();
        rootLayout.addComponent(content);
        content.setStyleName("view-content");
        content.setSizeFull();
        rootLayout.setStyleName("main-content");

        rootLayout.setExpandRatio(header, 1.0F);
        rootLayout.setExpandRatio(content, 2.0F);
        final Resource resource = context
                .getResource("classpath:/VAADIN/themes/" + UI.getCurrent().getTheme() + "/layouts/footer.html");

        try (InputStream resourceStream = resource.getInputStream()) {
            final CustomLayout customLayout = new CustomLayout(resourceStream);
            customLayout.setSizeUndefined();
            rootLayout.addComponent(customLayout);
        } catch (final IOException ex) {
            LOG.error("Footer file cannot be loaded", ex);
        }
        setContent(rootLayout);

        final Navigator navigator = new Navigator(this, content);
        navigator.addProvider(viewProvider);
        setNavigator(navigator);
    }

    private Component buildHeader() {
        final CssLayout cssLayout = new CssLayout();
        cssLayout.setStyleName("view-header");
        return cssLayout;
    }
}
