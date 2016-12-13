/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui;

import org.eclipse.hawkbit.ui.themes.HawkbitTheme;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Widgetset;
import com.vaadin.ui.UI;

/**
 * Abstract class for the ui to set widgetset and theme.
 */
@SuppressWarnings("serial")
@Widgetset(value = HawkbitTheme.WIDGET_SET_NAME)
@Theme(HawkbitTheme.THEME_NAME)
public abstract class DefaultHawkbitUI extends UI {

}
