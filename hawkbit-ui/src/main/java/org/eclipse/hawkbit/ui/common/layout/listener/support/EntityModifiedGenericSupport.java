/** 
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.layout.listener.support;

import java.util.Collection;
import java.util.function.Consumer;

import org.eclipse.hawkbit.ui.common.layout.listener.EntityModifiedListener.EntityModifiedAwareSupport;

/**
 * Support for entity modified
 */
public class EntityModifiedGenericSupport implements EntityModifiedAwareSupport {
    private final Consumer<Collection<Long>> onEntitiesAddedCallback;
    private final Consumer<Collection<Long>> onEntitiesUpdatedCallback;
    private final Consumer<Collection<Long>> onEntitiesDeletedCallback;

    /**
     * Constructor for EntityModifiedGenericSupport
     *
     * @param onEntitiesAddedCallback
     *            Entities added callback event
     * @param onEntitiesUpdatedCallback
     *            Entities updated callback event
     * @param onEntitiesDeletedCallback
     *            Entities deleted callback event
     */
    public EntityModifiedGenericSupport(final Consumer<Collection<Long>> onEntitiesAddedCallback,
            final Consumer<Collection<Long>> onEntitiesUpdatedCallback,
            final Consumer<Collection<Long>> onEntitiesDeletedCallback) {
        this.onEntitiesAddedCallback = onEntitiesAddedCallback;
        this.onEntitiesUpdatedCallback = onEntitiesUpdatedCallback;
        this.onEntitiesDeletedCallback = onEntitiesDeletedCallback;
    }

    /**
     * Static method for constructor EntityModifiedGenericSupport
     *
     * @param onEntitiesAddedCallback
     *            Entities added callback event
     * @param onEntitiesUpdatedCallback
     *            Entities updated callback event
     * @param onEntitiesDeletedCallback
     *            Entities deleted callback event
     *
     * @return Support for entity modified
     */
    public static EntityModifiedGenericSupport of(final Consumer<Collection<Long>> onEntitiesAddedCallback,
            final Consumer<Collection<Long>> onEntitiesUpdatedCallback,
            final Consumer<Collection<Long>> onEntitiesDeletedCallback) {
        return new EntityModifiedGenericSupport(onEntitiesAddedCallback, onEntitiesUpdatedCallback,
                onEntitiesDeletedCallback);
    }

    @Override
    public void onEntitiesAdded(final Collection<Long> entityIds) {
        if (onEntitiesAddedCallback != null) {
            onEntitiesAddedCallback.accept(entityIds);
        }
    }

    @Override
    public void onEntitiesUpdated(final Collection<Long> entityIds) {
        if (onEntitiesUpdatedCallback != null) {
            onEntitiesUpdatedCallback.accept(entityIds);
        }
    }

    @Override
    public void onEntitiesDeleted(final Collection<Long> entityIds) {
        if (onEntitiesDeletedCallback != null) {
            onEntitiesDeletedCallback.accept(entityIds);
        }
    }
}
