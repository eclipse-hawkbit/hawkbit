/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration.authentication;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;

/**
 * This class represents the UI item for the anonymous download by in the
 * authentication configuration view.
 */
@SpringComponent
@ViewScope
public class AnonymousDownloadAuthenticationConfigurationItem extends AbstractAuthenticationTenantConfigurationItem {

    private static final long serialVersionUID = 1L;

    private boolean configurationEnabled = false;
    private boolean configurationEnabledChange = false;

    @Autowired
    private I18N i18n;

    @Autowired
    public AnonymousDownloadAuthenticationConfigurationItem(
            final TenantConfigurationManagement tenantConfigurationManagement) {
        super(TenantConfigurationKey.ANONYMOUS_DOWNLOAD_MODE_ENABLED, tenantConfigurationManagement);
    }

    @PostConstruct
    public void init() {

        super.init(i18n.get("enonymous.download.label"));
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
