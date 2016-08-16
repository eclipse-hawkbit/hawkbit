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
import org.eclipse.hawkbit.security.SecurityTokenGenerator;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmall;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * This class represents the UI item for the gateway security token section in
 * the authentication configuration view.
 */
@SpringComponent
@ViewScope
public class GatewaySecurityTokenAuthenticationConfigurationItem extends AbstractAuthenticationTenantConfigurationItem {

    private static final long serialVersionUID = 1L;

    @Autowired
    private transient SecurityTokenGenerator securityTokenGenerator;

    private TextField gatewayTokenNameTextField;

    private Label gatewayTokenkeyLabel;

    private boolean configurationEnabled = false;
    private boolean configurationEnabledChange = false;

    private boolean keyNameChanged = false;

    private boolean keyChanged = false;

    private VerticalLayout detailLayout;

    /**
     * @param tenantConfigurationManagement
     */
    @Autowired
    public GatewaySecurityTokenAuthenticationConfigurationItem(
            final TenantConfigurationManagement tenantConfigurationManagement) {
        super(TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED, tenantConfigurationManagement);
    }

    /**
     * Init mehotd called by spring.
     */
    @PostConstruct
    public void init() {

        super.init("label.configuration.auth.gatewaytoken");
        configurationEnabled = isConfigEnabled();

        detailLayout = new VerticalLayout();
        detailLayout.setImmediate(true);
        gatewayTokenNameTextField = SPUIComponentProvider.getTextField(null, "", ValoTheme.TEXTFIELD_TINY, false, null,
                "", true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        gatewayTokenNameTextField.setImmediate(true);
        // hide text field until we support multiple gateway tokens for a tenan
        gatewayTokenNameTextField.setVisible(false);
        gatewayTokenNameTextField.addTextChangeListener(event -> doKeyNameChanged());

        final Button gatewaytokenBtn = SPUIComponentProvider.getButton("TODO-ID", "Regenerate Key", "",
                ValoTheme.BUTTON_TINY + " " + "redicon", true, null, SPUIButtonStyleSmall.class);
        gatewaytokenBtn.setImmediate(true);
        gatewaytokenBtn.setIcon(FontAwesome.REFRESH);
        gatewaytokenBtn.addClickListener(event -> generateGatewayToken());

        gatewayTokenkeyLabel = new LabelBuilder().id("gatewaysecuritytokenkey").name("").buildLabel();
        gatewayTokenkeyLabel.addStyleName("gateway-token-label");
        gatewayTokenkeyLabel.setImmediate(true);

        final HorizontalLayout keyGenerationLayout = new HorizontalLayout();
        keyGenerationLayout.setSpacing(true);
        keyGenerationLayout.setImmediate(true);

        keyGenerationLayout.addComponent(gatewayTokenNameTextField);
        keyGenerationLayout.addComponent(gatewayTokenkeyLabel);
        keyGenerationLayout.addComponent(gatewaytokenBtn);

        detailLayout.addComponent(keyGenerationLayout);

        if (isConfigEnabled()) {
            gatewayTokenNameTextField.setValue(getSecurityTokenName());
            gatewayTokenkeyLabel.setValue(getSecurityTokenKey());
            setDetailVisible(true);
        }
    }

    private void doKeyNameChanged() {
        keyNameChanged = true;
        notifyConfigurationChanged();
    }

    private void setDetailVisible(final boolean visible) {
        if (visible) {
            addComponent(detailLayout);
        } else {
            removeComponent(detailLayout);
        }

    }

    private void generateGatewayToken() {
        gatewayTokenkeyLabel.setValue(securityTokenGenerator.generateToken());
        keyChanged = true;
        notifyConfigurationChanged();
    }

    @Override
    public void configEnable() {
        if (!configurationEnabled) {
            configurationEnabledChange = true;
        }

        configurationEnabled = true;
        setDetailVisible(true);
        String gatewayTokenKey = getSecurityTokenKey();
        String gatewayTokenName = getSecurityTokenName();
        if (gatewayTokenKey == null) {
            gatewayTokenName = "GeneratedToken";
            keyNameChanged = true;
            gatewayTokenKey = securityTokenGenerator.generateToken();
            keyChanged = true;
        }
        gatewayTokenNameTextField.setValue(gatewayTokenName);
        gatewayTokenkeyLabel.setValue(gatewayTokenKey);
    }

    private String getSecurityTokenName() {
        return getTenantConfigurationManagement().getConfigurationValue(
                TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_NAME, String.class).getValue();
    }

    private String getSecurityTokenKey() {
        return getTenantConfigurationManagement().getConfigurationValue(
                TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY, String.class).getValue();
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
            getTenantConfigurationManagement().addOrUpdateConfiguration(
                    TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED, configurationEnabled);
        }

        if (keyNameChanged) {
            getTenantConfigurationManagement().addOrUpdateConfiguration(
                    TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_NAME,
                    gatewayTokenNameTextField.getValue());
        }
        if (keyChanged) {
            getTenantConfigurationManagement().addOrUpdateConfiguration(
                    TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY,
                    gatewayTokenkeyLabel.getValue());
        }
    }

    @Override
    public void undo() {
        configurationEnabledChange = false;
        keyNameChanged = false;
        keyChanged = false;
        gatewayTokenNameTextField.setValue(getSecurityTokenName());
        gatewayTokenkeyLabel.setValue(getSecurityTokenKey());
    }

}
