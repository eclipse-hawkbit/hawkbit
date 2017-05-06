/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.table;

import com.vaadin.ui.UI;

/**
 * Base class which every UI event should inherit from. The class provides
 * information about the view the event was published from.
 *
 */
public class BaseUIEvent {

    private final Class<?> source;

    /**
     * Constructor for BaseUIEvent
     */
    public BaseUIEvent() {
        this.source = UI.getCurrent().getNavigator().getCurrentView().getClass();
    }

    public Class<?> getSource() {
        return source;
    }

}
