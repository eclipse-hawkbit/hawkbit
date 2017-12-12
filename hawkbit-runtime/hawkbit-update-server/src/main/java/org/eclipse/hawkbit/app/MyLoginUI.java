/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.app;

import org.eclipse.hawkbit.im.authentication.MultitenancyIndicator;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.login.AbstractHawkbitLoginUI;
import org.eclipse.hawkbit.ui.themes.HawkbitTheme;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.vaadin.spring.security.VaadinSecurity;

import com.vaadin.spring.annotation.SpringUI;

/**
 * Example hawkBit login UI implementation.
 * 
 * A {@link SpringUI} annotated class must be present in the classpath for the
 * login path. The easiest way to get an hawkBit login UI running is to extend
 * the {@link AbstractHawkbitLoginUI} and to annotated it with {@link SpringUI}
 * as in this example to the defined {@link HawkbitTheme#LOGIN_UI_PATH}.
 */
@SpringUI(path = HawkbitTheme.LOGIN_UI_PATH)
// Exception squid:MaximumInheritanceDepth - Most of the inheritance comes from
// Vaadin.
@SuppressWarnings({ "squid:MaximumInheritanceDepth" })
public class MyLoginUI extends AbstractHawkbitLoginUI {
    private static final long serialVersionUID = 1L;

    @Autowired
    MyLoginUI(final ApplicationContext context, final VaadinSecurity vaadinSecurity, final VaadinMessageSource i18n,
            final UiProperties uiProperties, final MultitenancyIndicator multiTenancyIndicator) {
        super(context, vaadinSecurity, i18n, uiProperties, multiTenancyIndicator);
    }

}
