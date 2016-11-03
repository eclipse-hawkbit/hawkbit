/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.event;

import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.common.table.BaseUIEntityEvent;

/**
 * 
 *
 *
 *
 */
public class TargetAddUpdateWindowEvent extends BaseUIEntityEvent<Target> {

    /**
     * Constructor.
     * 
     * @param eventType
     *            the event type
     * @param entity
     *            the entity
     */
    public TargetAddUpdateWindowEvent(final BaseEntityEventType eventType, final Target entity) {
        super(eventType, entity);
    }

}
