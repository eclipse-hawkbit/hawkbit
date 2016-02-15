/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration.authentication;

import java.io.Serializable;

import com.vaadin.ui.Component;

/**
 * Interface to be implemented by any tenant specific configuration to show on
 * the UI.
 *
 *
 *
 *
 */
public interface TenantConfigurationItem extends Component {

    /**
     * @return {@code true} if configuration is enabled, otherwise {@code false}
     */
    boolean isConfigEnabled();

    /**
     * called when the configuration gets enabled.
     */
    void configEnable();

    /**
     * called when the configuration is disabled.
     */
    void configDisable();

    /**
     * called to save the configuration.
     */
    void save();

    /**
     * called to rollback made changes.
     */
    void undo();

    /**
     * Adds a configuration change listener to notify about configuration
     * changes.
     *
     * @param listener
     *            the listener to be notified in case the item changes some
     *            configuration
     */
    void addConfigurationChangeListener(TenantConfigurationChangeListener listener);

    /**
     * Configuration Change Listener to be notified about configuration changes
     * in configuration item.
     *
     *
     *
     */
    @FunctionalInterface
    interface TenantConfigurationChangeListener extends Serializable {
        /**
         * called to notify about configuration has been changed.
         */
        void configurationHasChanged();
    }
}
