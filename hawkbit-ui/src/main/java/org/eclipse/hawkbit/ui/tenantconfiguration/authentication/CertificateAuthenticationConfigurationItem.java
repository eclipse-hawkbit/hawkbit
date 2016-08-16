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
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * This class represents the UI item for the certificate authenticated by an
 * reverse proxy in the authentication configuration view.
 */
@SpringComponent
@ViewScope
public class CertificateAuthenticationConfigurationItem extends AbstractAuthenticationTenantConfigurationItem {

    private static final long serialVersionUID = 1L;

    private boolean configurationEnabled = false;
    private boolean configurationEnabledChange = false;
    private boolean configurationCaRootAuthorityChanged = false;

    private VerticalLayout detailLayout;
    private TextField caRootAuthorityTextField;

    @Autowired
    public CertificateAuthenticationConfigurationItem(
            final TenantConfigurationManagement tenantConfigurationManagement) {
        super(TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_ENABLED, tenantConfigurationManagement);
    }

    /**
     * Init mehotd called by spring.
     */
    @PostConstruct
    public void init() {
        super.init("label.configuration.auth.header");
        configurationEnabled = isConfigEnabled();

        detailLayout = new VerticalLayout();
        detailLayout.setImmediate(true);

        final HorizontalLayout caRootAuthorityLayout = new HorizontalLayout();
        caRootAuthorityLayout.setSpacing(true);

        final Label caRootAuthorityLabel = new LabelBuilder().name("SSL Issuer Hash:").buildLabel();
        caRootAuthorityLabel.setDescription(
                "The SSL Issuer iRules.X509 hash, to validate against the controller request certifcate.");

        caRootAuthorityTextField = SPUIComponentProvider.getTextField(null, "", ValoTheme.TEXTFIELD_TINY, false, null,
                "", true, 128);
        caRootAuthorityTextField.setWidth("500px");
        caRootAuthorityTextField.setImmediate(true);
        caRootAuthorityTextField.addTextChangeListener(event -> caRootAuthorityChanged());

        caRootAuthorityLayout.addComponent(caRootAuthorityLabel);
        caRootAuthorityLayout.addComponent(caRootAuthorityTextField);

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
