/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration.authentication;

import org.eclipse.hawkbit.ui.tenantconfiguration.ConfigurationItem;

import com.vaadin.ui.Component;

/**
 * Interface to be implemented by any tenant specific configuration to show on
 * the UI.
 *
 *
 *
 *
 */
public interface AuthenticationConfigurationItem extends Component, ConfigurationItem {

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

}
