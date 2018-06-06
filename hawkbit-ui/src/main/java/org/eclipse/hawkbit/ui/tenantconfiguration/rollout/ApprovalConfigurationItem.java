/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration.rollout;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.eclipse.hawkbit.ui.tenantconfiguration.generic.AbstractBooleanTenantConfigurationItem;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

/**
 * This class represents the UI item for the target security token section in
 * the authentication configuration view.
 */
public class ApprovalConfigurationItem extends AbstractBooleanTenantConfigurationItem {

    private static final long serialVersionUID = 1L;

    private boolean configurationEnabled;
    private boolean configurationEnabledChange;

    /**
     *  Constructor for tenant specific approval mode setting.
     *
     * @param tenantConfigurationManagement used to enable/disable the approval mode per tenant
     * @param i18n used to translate labels
     */
    public ApprovalConfigurationItem(final TenantConfigurationManagement tenantConfigurationManagement,
            final VaadinMessageSource i18n) {
        super(TenantConfigurationKey.ROLLOUT_APPROVAL_ENABLED, tenantConfigurationManagement, i18n);

        super.init("configuration.rollout.approval.label");
        configurationEnabled = isConfigEnabled();
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
