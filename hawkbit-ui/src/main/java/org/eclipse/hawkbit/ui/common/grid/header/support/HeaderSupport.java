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

    Component getHeaderComponent();

    default float getExpandRation() {
        return 0F;
    }

    default void restoreState() {
        // empty by default for stateless header supports
    }
}
