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
 * Table Horizonatl layout with Style.
 * 
 *
 *
 */
public class SPUITableHorizonatlLayout {

    private HorizontalLayout tableHorizonatlLayout;

    /**
     * Constructor.
     */
    public SPUITableHorizonatlLayout() {
        tableHorizonatlLayout = new HorizontalLayout();
        decorate();
    }

    /**
     * Decorate.
     */
    private void decorate() {
        tableHorizonatlLayout.setSpacing(false);
        tableHorizonatlLayout.setMargin(false);
    }

    public HorizontalLayout getTableHorizonatlLayout() {
        return tableHorizonatlLayout;
    }
}
