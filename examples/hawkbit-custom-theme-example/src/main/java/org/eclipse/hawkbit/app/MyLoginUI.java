/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.app;

import org.eclipse.hawkbit.ui.login.AbstractHawkbitLoginUI;
import org.eclipse.hawkbit.ui.themes.HawkbitTheme;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.navigator.SpringViewProvider;

/**
 * Example hawkBit login UI implementation.
 * 
 * A {@link SpringUI} annotated class must be present in the classpath for the
 * login path. The easiest way to get an hawkBit login UI running is to extend
 * the {@link AbstractHawkbitLoginUI} and to annotated it with {@link SpringUI} as in
 * this example to the defined {@link HawkbitTheme#LOGIN_UI_PATH}.
 * 
 */
@SpringUI(path = HawkbitTheme.LOGIN_UI_PATH)
@Title("hawkBit Theme example")
@Theme(value = "exampletheme")
public class MyLoginUI extends AbstractHawkbitLoginUI {

    private static final long serialVersionUID = 1L;

    @Autowired
    protected MyLoginUI(final SpringViewProvider viewProvider, final ApplicationContext context) {
        super(viewProvider, context);
    }

}
