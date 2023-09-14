/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.layout.listener.support;

import java.util.Collection;

import org.eclipse.hawkbit.ui.common.layout.CountAwareComponent;
import org.eclipse.hawkbit.ui.common.layout.listener.EntityModifiedListener.EntityModifiedAwareSupport;

/**
 * Support for entity modified adapting entities count.
 */
public class EntityModifiedCountAwareSupport implements EntityModifiedAwareSupport {
    private final CountAwareComponent countAwareComponent;

    /**
     * Constructor.
     *
     * @param countAwareComponent
     *            Component that adapts its count
     */
    public EntityModifiedCountAwareSupport(final CountAwareComponent countAwareComponent) {
        this.countAwareComponent = countAwareComponent;
    }

    /**
     * Static method for constructing EntityModifiedCountSupport
     *
     * @param countAwareComponent
     *            Component that adapts its count
     * @return instance of {@link EntityModifiedCountAwareSupport}
     */
    public static EntityModifiedCountAwareSupport of(final CountAwareComponent countAwareComponent) {
        return new EntityModifiedCountAwareSupport(countAwareComponent);
    }

    @Override
    public void onEntitiesAdded(final Collection<Long> entityIds) {
        countAwareComponent.updateCountOnEntitiesAdded(entityIds.size());
    }

    @Override
    public void onEntitiesUpdated(final Collection<Long> entityIds) {
        countAwareComponent.updateCountOnEntitiesUpdated();
    }

    @Override
    public void onEntitiesDeleted(final Collection<Long> entityIds) {
        countAwareComponent.updateCountOnEntitiesDeleted(entityIds.size());
    }
}
