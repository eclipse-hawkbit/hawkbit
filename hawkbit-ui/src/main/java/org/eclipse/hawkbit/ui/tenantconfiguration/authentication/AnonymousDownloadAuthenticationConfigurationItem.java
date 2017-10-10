/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration.authentication;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.eclipse.hawkbit.ui.tenantconfiguration.generic.AbstractBooleanTenantConfigurationItem;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

/**
 * This class represents the UI item for the anonymous download by in the
 * authentication configuration view.
 */
public class AnonymousDownloadAuthenticationConfigurationItem extends AbstractBooleanTenantConfigurationItem {

    private static final long serialVersionUID = 1L;

    private boolean configurationEnabled;
    private boolean configurationEnabledChange;

    public AnonymousDownloadAuthenticationConfigurationItem(
            final TenantConfigurationManagement tenantConfigurationManagement, final VaadinMessageSource i18n) {
        super(TenantConfigurationKey.ANONYMOUS_DOWNLOAD_MODE_ENABLED, tenantConfigurationManagement, i18n);

        super.init("label.configuration.anonymous.download");
        configurationEnabled = isConfigEnabled();
    }

    @Override
    public void configEnable() {
        configurationEnabledChange = !configurationEnabled;
        configurationEnabled = true;
    }

    @Override
    public void configDisable() {
        configurationEnabledChange = configurationEnabled;
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
