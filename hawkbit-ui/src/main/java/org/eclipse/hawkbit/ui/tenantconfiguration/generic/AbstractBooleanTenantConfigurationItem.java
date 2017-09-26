/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration.generic;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.VerticalLayout;

/**
 * abstract authentication configuration item.
 */
public abstract class AbstractBooleanTenantConfigurationItem extends VerticalLayout
        implements BooleanConfigurationItem {

    private static final long serialVersionUID = 1L;

    private final VaadinMessageSource i18n;

    private final String configurationKey;
    private final transient TenantConfigurationManagement tenantConfigurationManagement;

    private final List<ConfigurationItemChangeListener> configurationChangeListeners = new ArrayList<>();

    /**
     * @param configurationKey
     *            the key for this configuration
     * @param tenantConfigurationManagement
     *            the tenant configuration management to retrieve the
     *            configuration value
     */
    protected AbstractBooleanTenantConfigurationItem(final String configurationKey,
            final TenantConfigurationManagement tenantConfigurationManagement, final VaadinMessageSource i18n) {
        this.configurationKey = configurationKey;
        this.tenantConfigurationManagement = tenantConfigurationManagement;
        this.i18n = i18n;
    }

    /**
     * initialize the abstract component.
     */
    protected void init(final String labelText) {
        setImmediate(true);
        addComponent(new LabelBuilder().name(i18n.getMessage(labelText)).buildLabel());
    }

    @Override
    public boolean isConfigEnabled() {
        final TenantConfigurationValue<Boolean> enabled = tenantConfigurationManagement
                .getConfigurationValue(configurationKey, Boolean.class);

        return enabled.getValue() && !enabled.isGlobal();
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
    protected String getConfigurationKey() {
        return configurationKey;
    }

    protected void notifyConfigurationChanged() {
        configurationChangeListeners.forEach(ConfigurationItemChangeListener::configurationHasChanged);
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
