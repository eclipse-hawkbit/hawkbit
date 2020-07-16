/**
 * Copyright (c) 2020 devolo AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration.search;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.ui.tenantconfiguration.generic.AbstractBooleanTenantConfigurationItem;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.*;

public class TargetSearchConfigurationItem extends AbstractBooleanTenantConfigurationItem {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(TargetSearchConfigurationItem.class);

    private boolean configurationEnabled;
    private boolean configurationEnabledChange;

    public TargetSearchConfigurationItem(TenantConfigurationManagement tenantConfigurationManagement, VaadinMessageSource i18n) {
        super(TenantConfigurationKey.TARGET_SEARCH_ATTRIBUTES_ENABLED, tenantConfigurationManagement, i18n);

        super.init("configuration.targetsearch.attributes.label");
        this.configurationEnabled = isConfigEnabled();
    }

    @Override
    public void configEnable() {
        if (!configurationEnabled) {
            configurationEnabledChange = true;
        }
        configurationEnabled = true;
    }

    @Override
    public void configDisable() {
        if (configurationEnabled) {
            configurationEnabledChange = true;
        }
        configurationEnabled = false;
    }

    @Override
    public void save() {
        if (!configurationEnabledChange) {
            return;
        }
        getTenantConfigurationManagement().addOrUpdateConfiguration(getConfigurationKey(), configurationEnabled);
    }

    @Override
    public void undo() {
        configurationEnabledChange = false;
        configurationEnabled = getTenantConfigurationManagement()
                .getConfigurationValue(getConfigurationKey(), Boolean.class).getValue();
    }
}
