/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.grid.header.support;

import com.vaadin.ui.Component;

/**
 * Interface for header support component
 */
@FunctionalInterface
public interface HeaderSupport {

    /**
     * Header support component.
     * 
     * @return header support component
     */
    Component getHeaderComponent();

    /**
     * Header support component expand ration.
     * 
     * @return expand ration
     */
    default float getExpandRation() {
        return 0F;
    }

    /**
     * Callback to restore header support component state.
     */
    default void restoreState() {
        // empty by default for stateless header supports
    }
}
