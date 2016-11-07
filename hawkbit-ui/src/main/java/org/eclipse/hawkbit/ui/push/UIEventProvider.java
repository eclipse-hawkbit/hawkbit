/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.push;

import java.util.Collections;
import java.util.Map;

import org.eclipse.hawkbit.repository.event.TenantAwareEvent;

/**
 * The UI event provider hold all supported repository events which will
 * delegated to the UI.
 */
public interface UIEventProvider {

    /**
     * Return all supported repository event types. All events which this type
     * are delegated to the UI as list of events.
     * 
     * @return list of provided event types. Should not be null
     */
    default Map<Class<? extends TenantAwareEvent>, Class<?>> getEvents() {
        return Collections.emptyMap();
    }

}
