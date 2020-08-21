/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
