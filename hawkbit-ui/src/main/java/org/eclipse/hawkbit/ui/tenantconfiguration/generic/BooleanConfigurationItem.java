/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
