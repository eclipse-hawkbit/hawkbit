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
 * Support for refresh the grid on entity modified
 */
public class EntityModifiedGridRefreshAwareSupport implements EntityModifiedAwareSupport {
    private final Runnable refreshGridCallback;
    private final Consumer<Collection<Long>> refreshGridItemsCallback;

    /**
     * Constructor for EntityModifiedGridRefreshAwareSupport
     *
     * @param refreshGridCallback
     *            Refresh grid callback event
     * @param refreshGridItemsCallback
     *            Refresh grid items callback event
     */
    public EntityModifiedGridRefreshAwareSupport(final Runnable refreshGridCallback,
            final Consumer<Collection<Long>> refreshGridItemsCallback) {
        this.refreshGridCallback = refreshGridCallback;
        this.refreshGridItemsCallback = refreshGridItemsCallback;
    }

    /**
     * Static method for constructor EntityModifiedGridRefreshAwareSupport
     *
     * @param refreshGridCallback
     *            Refresh grid callback event
     *
     * @return Support for refresh the grid on entity modified
     */
    public static EntityModifiedGridRefreshAwareSupport of(final Runnable refreshGridCallback) {
        return of(refreshGridCallback, null);
    }

    /**
     * Static method for constructor EntityModifiedGridRefreshAwareSupport
     *
     * @param refreshGridCallback
     *            Refresh grid callback event
     * @param refreshGridItemsCallback
     *            Refresh grid items callback event
     *
     * @return Support for refresh the grid on entity modified
     */
    public static EntityModifiedGridRefreshAwareSupport of(final Runnable refreshGridCallback,
            final Consumer<Collection<Long>> refreshGridItemsCallback) {
        return new EntityModifiedGridRefreshAwareSupport(refreshGridCallback, refreshGridItemsCallback);
    }

    @Override
    public void onEntitiesAdded(final Collection<Long> entityIds) {
        refreshGridCallback.run();
    }

    @Override
    public void onEntitiesUpdated(final Collection<Long> entityIds) {
        if (refreshGridItemsCallback == null) {
            refreshGridCallback.run();
        } else {
            refreshGridItemsCallback.accept(entityIds);
        }
    }

    @Override
    public void onEntitiesDeleted(final Collection<Long> entityIds) {
        refreshGridCallback.run();
    }
}
