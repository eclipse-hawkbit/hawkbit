/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration.generic;

import org.eclipse.hawkbit.ui.tenantconfiguration.ConfigurationGroup;

/**
 * Interface to be implemented by any tenant specific configuration to show on
 * the UI.
 */
public interface BooleanConfigurationItem extends ConfigurationGroup {

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

}
