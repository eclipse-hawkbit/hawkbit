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

import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.model.TenantConfiguration;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;

/**
 *
 *
 *
 */
@SpringComponent
@ViewScope
public class TargetSecurityTokenAuthenticationConfigurationItem extends AbstractAuthenticationTenantConfigurationItem {

    /**
    *
    */
    private static final long serialVersionUID = 1L;

    @Autowired
    private I18N i18n;

    private boolean configurationEnabled = false;
    private boolean configurationEnabledChange = false;

    /**
     * @param systemManagement
     *            the system management to retrie the configuration
     */
    @Autowired
    public TargetSecurityTokenAuthenticationConfigurationItem(final SystemManagement systemManagement) {
        super(TenantConfigurationKey.AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED, systemManagement);
    }

    /**
     * init mehotd called by spring.
     */
    @PostConstruct
    public void init() {
        super.init(i18n.get("label.configuration.auth.targettoken"));
        configurationEnabled = isConfigEnabled();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.hawkbit.server.ui.tenantconfiguration.
     * TenantConfigurationItem# configEnable()
     */
    @Override
    public void configEnable() {
        if (!configurationEnabled) {
            configurationEnabledChange = true;
        }
        configurationEnabled = true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.hawkbit.server.ui.tenantconfiguration.
     * TenantConfigurationItem# configDisable()
     */
    @Override
    public void configDisable() {
        if (configurationEnabled) {
            configurationEnabledChange = true;
        }
        configurationEnabled = false;
    }

    @Override
    public void save() {
        if (configurationEnabledChange) {
            getSystemManagement().addOrUpdateConfiguration(
                    new TenantConfiguration(getConfigurationKey().getKeyName(), String.valueOf(configurationEnabled)));
        }
    }

    @Override
    public void undo() {
        configurationEnabledChange = false;
        configurationEnabled = getSystemManagement().getConfigurationValue(getConfigurationKey(), Boolean.class)
                .getValue();
    }
}
