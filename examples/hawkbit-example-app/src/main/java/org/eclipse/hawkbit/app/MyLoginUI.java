/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.app;

import org.eclipse.hawkbit.ui.login.HawkbitLoginUI;
import org.eclipse.hawkbit.ui.themes.HawkbitTheme;

import com.vaadin.spring.annotation.SpringUI;

/**
 * Example hawkBit login UI implementation.
 * 
 * A {@link SpringUI} annotated class must be present in the classpath for the
 * login path. The easiest way to get an hawkBit login UI running is to extend
 * the {@link HawkbitLoginUI} and to annotated it with {@link SpringUI} as in
 * this example to the defined {@link HawkbitTheme#LOGIN_UI_PATH}.
 * 
 *
 *
 */
@SpringUI(path = HawkbitTheme.LOGIN_UI_PATH)
public class MyLoginUI extends HawkbitLoginUI {

    private static final long serialVersionUID = 1L;

}
