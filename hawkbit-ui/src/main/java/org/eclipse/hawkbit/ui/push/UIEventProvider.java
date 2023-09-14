/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.push;

import java.util.Collections;
import java.util.Map;

import org.eclipse.hawkbit.repository.event.entity.EntityIdEvent;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayloadIdentifier;

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
    default Map<Class<? extends EntityIdEvent>, EntityModifiedEventPayloadIdentifier> getEvents() {
        return Collections.emptyMap();
    }

}
