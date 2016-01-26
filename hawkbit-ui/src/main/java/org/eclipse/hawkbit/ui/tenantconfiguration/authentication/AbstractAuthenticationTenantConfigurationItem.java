/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration.authentication;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;

import com.vaadin.ui.VerticalLayout;

/**
 * Ab abstract authentication configuration item.
 *
 *
 *
 *
 */
abstract class AbstractAuthenticationTenantConfigurationItem extends VerticalLayout
        implements AuthenticationConfigurationItem {

    /**
    *
    */
    private static final long serialVersionUID = 1L;

    private final TenantConfigurationKey configurationKey;
    private final transient SystemManagement systemManagement;

    private final List<ConfigurationItemChangeListener> configurationChangeListeners = new ArrayList<>();

    /**
     * @param configurationKey
     *            the key for this configuration
     * @param systemManagement
     *            the system management to retrive the configuration value
     */
    public AbstractAuthenticationTenantConfigurationItem(final TenantConfigurationKey configurationKey,
            final SystemManagement systemManagement) {
        this.configurationKey = configurationKey;
        this.systemManagement = systemManagement;
    }

    /**
     * initialize the abstract component.
     */
    protected void init(final String labelText) {
        setImmediate(true);
        addComponent(SPUIComponentProvider.getLabel(labelText, SPUILabelDefinitions.SP_LABEL_SIMPLE));
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.hawkbit.server.ui.tenantconfiguration.
     * TenantConfigurationItem# isConfigEnabled()
     */
    @Override
    public boolean isConfigEnabled() {
        final boolean b = systemManagement.getConfigurationValue(configurationKey, Boolean.class).getValue();
        return b;
    }

    /**
     * @return the systemManagement
     */
    protected SystemManagement getSystemManagement() {
        return systemManagement;
    }

    /**
     * @return the configurationKey
     */
    protected TenantConfigurationKey getConfigurationKey() {
        return configurationKey;
    }

    protected void notifyConfigurationChanged() {
        configurationChangeListeners.forEach(listener -> listener.configurationHasChanged());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.hawkbit.server.ui.tenantconfiguration.
     * TenantConfigurationItem# addConfigurationChangeListener
     * (hawkbit.server.ui.tenantconfiguration.TenantConfigurationItem.
     * TenantConfigurationChangeListener)
     */
    @Override
    public void addChangeListener(final ConfigurationItemChangeListener listener) {
        configurationChangeListeners.add(listener);
    }

    @Override
    public boolean isUserInputValid() {
        return true;
    }
}
