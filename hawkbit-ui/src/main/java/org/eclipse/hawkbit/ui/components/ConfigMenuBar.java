/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.components;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.themes.ValoTheme;

public class ConfigMenuBar extends MenuBar {

    private final MenuItem config;

    public ConfigMenuBar() {
        setStyleName(ValoTheme.MENUBAR_BORDERLESS);
        addStyleName("menubar-position");
        config = addItem("", FontAwesome.COG, null);
        config.setStyleName("tags");
    }

    public MenuItem getConfig() {
        return config;
    }

}
