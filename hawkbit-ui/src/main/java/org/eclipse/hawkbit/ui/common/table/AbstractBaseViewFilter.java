/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.table;

import org.vaadin.spring.events.EventBusListenerMethodFilter;

/**
 * Abstract class for filter classes which contains information about the origin
 * view.
 *
 */
public abstract class AbstractBaseViewFilter implements EventBusListenerMethodFilter {

    @Override
    public boolean filter(final Object payload) {

        if (!(payload instanceof BaseUIEvent)) {
            return false;
        }

        final BaseUIEvent event = (BaseUIEvent) payload;

        return getOriginView().equals(event.getSource());
    }

    protected abstract Class<?> getOriginView();

}
