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

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;

import com.vaadin.ui.VerticalLayout;

/**
 * abstract authentication configuration item.
 *
 *
 *
 *
 */
abstract class AbstractAuthenticationTenantConfigurationItem extends VerticalLayout
        implements AuthenticationConfigurationItem {

    private static final long serialVersionUID = 1L;

    private final TenantConfigurationKey configurationKey;
    private final transient TenantConfigurationManagement tenantConfigurationManagement;

    private final List<ConfigurationItemChangeListener> configurationChangeListeners = new ArrayList<>();

    /**
     * @param configurationKey
     *            the key for this configuration
     * @param tenantConfigurationManagement
     *            the tenant configuration management to retrieve the
     *            configuration value
     */
    public AbstractAuthenticationTenantConfigurationItem(final TenantConfigurationKey configurationKey,
            final TenantConfigurationManagement tenantConfigurationManagement) {
        this.configurationKey = configurationKey;
        this.tenantConfigurationManagement = tenantConfigurationManagement;
    }

    /**
     * initialize the abstract component.
     */
    protected void init(final String labelText) {
        setImmediate(true);
        addComponent(SPUIComponentProvider.getLabel(labelText, SPUILabelDefinitions.SP_LABEL_SIMPLE));
    }

    @Override
    public boolean isConfigEnabled() {
        return tenantConfigurationManagement.getConfigurationValue(configurationKey, Boolean.class).getValue();
    }

    /**
     * @return the systemManagement
     */
    protected TenantConfigurationManagement getTenantConfigurationManagement() {
        return tenantConfigurationManagement;
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

    @Override
    public void addChangeListener(final ConfigurationItemChangeListener listener) {
        configurationChangeListeners.add(listener);
    }

    @Override
    public boolean isUserInputValid() {
        return true;
    }
}
