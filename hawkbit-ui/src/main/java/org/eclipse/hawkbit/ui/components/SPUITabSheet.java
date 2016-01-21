/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.components;

import com.vaadin.ui.TabSheet;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Tabs required for discription - Down arrow.
 * 
 *
 *
 *
 */
@SuppressWarnings("serial")
public class SPUITabSheet extends TabSheet {

    /**
     * Constructor.
     */
    public SPUITabSheet() {
        super();
        decorate();
    }

    /**
     * Decorate.
     */
    private void decorate() {
        addStyleName(ValoTheme.TABSHEET_COMPACT_TABBAR);
        addStyleName(ValoTheme.TABSHEET_FRAMED);
    }
}
