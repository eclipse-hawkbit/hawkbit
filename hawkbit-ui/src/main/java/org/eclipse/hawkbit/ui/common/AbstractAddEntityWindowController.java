/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common;

import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;

/**
 * Window controller for entity creations.
 *
 * @param <T>
 *            Type of proxy entity
 * @param <E>
 *            Second type of proxy entity
 * @param <R>
 *            Type of repository entity
 */
public abstract class AbstractAddEntityWindowController<T, E, R> extends AbstractEntityWindowController<T, E, R> {

    /**
     * Constructor
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     */
    protected AbstractAddEntityWindowController(final CommonUiDependencies uiDependencies) {
        super(uiDependencies);
    }

    @Override
    protected String getPersistSuccessMessageKey() {
        return "message.save.success";
    }

    @Override
    protected String getPersistFailureMessageKey() {
        return "message.save.fail";
    }

    @Override
    protected EntityModifiedEventPayload createModifiedEventPayload(final R entity) {
        return new EntityModifiedEventPayload(EntityModifiedEventType.ENTITY_ADDED, getParentEntityClass(),
                getEntityClass(), getId(entity));
    }
}
