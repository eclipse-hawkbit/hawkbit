/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;

/**
 * Window controller for entity updates.
 *
 * @param <T>
 *            Type of proxy entity
 * @param <E>
 *            Second type of proxy entity
 * @param <R>
 *            Type of repository entity
 */
public abstract class AbstractUpdateEntityWindowController<T, E, R> extends AbstractEntityWindowController<T, E, R> {

    /**
     * Constructor
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     */
    protected AbstractUpdateEntityWindowController(final CommonUiDependencies uiDependencies) {
        super(uiDependencies);
    }

    @Override
    protected String getPersistSuccessMessageKey() {
        return "message.update.success";
    }

    @Override
    protected String getPersistFailureMessageKey() {
        return "message.deleted.or.notAllowed";
    }

    @Override
    protected EntityModifiedEventPayload createModifiedEventPayload(final R entity) {
        return new EntityModifiedEventPayload(EntityModifiedEventType.ENTITY_UPDATED, getParentEntityClass(),
                getEntityClass(), getId(entity));
    }
}
