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
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.tenantconfiguration.generic.AbstractBooleanTenantConfigurationItem;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 * This class represents the UI item for the certificate authenticated by an
 * reverse proxy in the authentication configuration view.
 */
public class CertificateAuthenticationConfigurationItem extends AbstractBooleanTenantConfigurationItem {

    private static final long serialVersionUID = 1L;

    private boolean configurationEnabled;
    private boolean configurationEnabledChange;
    private boolean configurationCaRootAuthorityChanged;

    private final VerticalLayout detailLayout;
    private final TextField caRootAuthorityTextField;

    public CertificateAuthenticationConfigurationItem(final TenantConfigurationManagement tenantConfigurationManagement,
            final VaadinMessageSource i18n) {
        super(TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_ENABLED, tenantConfigurationManagement, i18n);

        super.init("label.configuration.auth.header");
        configurationEnabled = isConfigEnabled();

        detailLayout = new VerticalLayout();
        detailLayout.setImmediate(true);

        final HorizontalLayout caRootAuthorityLayout = new HorizontalLayout();
        caRootAuthorityLayout.setSpacing(true);

        final Label caRootAuthorityLabel = new LabelBuilder().name("SSL Issuer Hash:").buildLabel();
        caRootAuthorityLabel.setDescription(
                "The SSL Issuer iRules.X509 hash, to validate against the controller request certifcate.");
        caRootAuthorityLabel.setWidthUndefined();

        caRootAuthorityTextField = new TextFieldBuilder().immediate(true).maxLengthAllowed(160).buildTextComponent();
        caRootAuthorityTextField.setWidth("100%");
        caRootAuthorityTextField.addTextChangeListener(event -> caRootAuthorityChanged());

        caRootAuthorityLayout.addComponent(caRootAuthorityLabel);
        caRootAuthorityLayout.setExpandRatio(caRootAuthorityLabel, 0);
        caRootAuthorityLayout.addComponent(caRootAuthorityTextField);
        caRootAuthorityLayout.setExpandRatio(caRootAuthorityTextField, 1);
        caRootAuthorityLayout.setWidth("100%");

        detailLayout.addComponent(caRootAuthorityLayout);

        if (isConfigEnabled()) {
            caRootAuthorityTextField.setValue(getCaRootAuthorityValue());
            setDetailVisible(true);
        }
    }

    @Override
    public void configEnable() {
        if (!configurationEnabled) {
            configurationEnabledChange = true;
        }
        configurationEnabled = true;
        configurationCaRootAuthorityChanged = true;
        setDetailVisible(true);
    }

    @Override
    public void configDisable() {
        if (configurationEnabled) {
            configurationEnabledChange = true;
        }
        configurationEnabled = false;
        setDetailVisible(false);
    }

    @Override
    public void save() {
        if (configurationEnabledChange) {
            getTenantConfigurationManagement().addOrUpdateConfiguration(getConfigurationKey(), configurationEnabled);
        }
        if (configurationCaRootAuthorityChanged) {
            final String value = caRootAuthorityTextField.getValue() != null ? caRootAuthorityTextField.getValue() : "";
            getTenantConfigurationManagement()
                    .addOrUpdateConfiguration(TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_AUTHORITY_NAME, value);
        }
    }

    @Override
    public void undo() {
        configurationEnabledChange = false;
        configurationCaRootAuthorityChanged = false;

        configurationEnabled = getTenantConfigurationManagement()
                .getConfigurationValue(getConfigurationKey(), Boolean.class).getValue();
        caRootAuthorityTextField.setValue(getCaRootAuthorityValue());
    }

    private String getCaRootAuthorityValue() {
        return getTenantConfigurationManagement()
                .getConfigurationValue(TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_AUTHORITY_NAME, String.class)
                .getValue();
    }

    private void setDetailVisible(final boolean visible) {
        if (visible) {
            addComponent(detailLayout);
        } else {
            removeComponent(detailLayout);
        }

    }

    private void caRootAuthorityChanged() {
        configurationCaRootAuthorityChanged = true;
        notifyConfigurationChanged();
    }

}
