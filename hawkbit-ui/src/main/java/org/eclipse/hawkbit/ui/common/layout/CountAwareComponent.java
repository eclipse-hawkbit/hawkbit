/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.layout;

/**
 * Interface for count aware component.
 *
 */
public interface CountAwareComponent {

    /**
     * Adapts count on entities added.
     *
     * @param count
     *            added entities count
     */
    void updateCountOnEntitiesAdded(final int count);

    /**
     * Adapts count on entities updated.
     *
     */
    void updateCountOnEntitiesUpdated();

    /**
     * Adapts count on entities deleted.
     *
     * @param count
     *            deleted entities count
     */
    void updateCountOnEntitiesDeleted(final int count);
}
