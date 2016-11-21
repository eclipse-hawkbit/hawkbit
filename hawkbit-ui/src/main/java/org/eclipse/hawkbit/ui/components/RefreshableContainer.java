/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ui.components;

/**
 * Components which are refreshable.
 */
@FunctionalInterface
public interface RefreshableContainer {

    /**
     * Refresh the container.
     */
    void refreshContainer();

}
