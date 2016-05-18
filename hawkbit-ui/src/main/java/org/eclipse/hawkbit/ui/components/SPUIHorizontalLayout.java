/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.components;

import com.vaadin.ui.HorizontalLayout;

/**
 * Plain Horizontal Layout.
 * 
 *
 *
 */

public class SPUIHorizontalLayout {
    private final HorizontalLayout uiHorizontalLayout;

    /**
     * Default constructor.
     */
    public SPUIHorizontalLayout() {
        uiHorizontalLayout = new HorizontalLayout();
        decorate();
    }

    /**
     * Decorate.
     */
    private void decorate() {
        uiHorizontalLayout.setImmediate(false);
        uiHorizontalLayout.setMargin(false);
        uiHorizontalLayout.setSpacing(true);

    }

    /**
     * Get HorizontalLayout.
     * 
     * @return HorizontalLayout as UI
     */
    public HorizontalLayout getUiHorizontalLayout() {
        return uiHorizontalLayout;
    }
}
