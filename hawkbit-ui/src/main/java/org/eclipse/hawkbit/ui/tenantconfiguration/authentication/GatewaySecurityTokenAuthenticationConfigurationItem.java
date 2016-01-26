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
import org.eclipse.hawkbit.security.SecurityTokenGenerator;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmall;
import org.eclipse.hawkbit.ui.utils.I18N;
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
 *
 *
 */
@SpringComponent
@ViewScope
public class GatewaySecurityTokenAuthenticationConfigurationItem extends AbstractAuthenticationTenantConfigurationItem {

    /**
    *
    */
    private static final long serialVersionUID = 1L;

    @Autowired
    private transient SecurityTokenGenerator securityTokenGenerator;
    @Autowired
    private I18N i18n;

    private TextField gatewayTokenNameTextField;

    private Label gatewayTokenkeyLabel;

    private boolean configurationEnabled = false;
    private boolean configurationEnabledChange = false;

    private boolean keyNameChanged = false;

    private boolean keyChanged = false;

    private VerticalLayout detailLayout;

    /**
     * @param configurationKey
     * @param systemManagement
     */
    @Autowired
    public GatewaySecurityTokenAuthenticationConfigurationItem(final SystemManagement systemManagement) {
        super(TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED, systemManagement);
    }

    /**
     * init mehotd called by spring.
     */
    @PostConstruct
    public void init() {

        super.init(i18n.get("label.configuration.auth.gatewaytoken"));
        configurationEnabled = isConfigEnabled();

        detailLayout = new VerticalLayout();
        detailLayout.setImmediate(true);
        gatewayTokenNameTextField = SPUIComponentProvider.getTextField("", ValoTheme.TEXTFIELD_TINY, false, null, "",
                true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        gatewayTokenNameTextField.setImmediate(true);
        // hide text field until we support multiple gateway tokens for a tenant
        // MECS-830
        gatewayTokenNameTextField.setVisible(false);
        gatewayTokenNameTextField.addTextChangeListener(event -> keyNameChanged());

        final Button gatewaytokenBtn = SPUIComponentProvider.getButton("TODO-ID", "Regenerate Key", "",
                ValoTheme.BUTTON_TINY + " " + "redicon", true, null, SPUIButtonStyleSmall.class);
        gatewaytokenBtn.setImmediate(true);
        gatewaytokenBtn.setIcon(FontAwesome.REFRESH);
        gatewaytokenBtn.addClickListener(event -> generateGatewayToken());

        gatewayTokenkeyLabel = SPUIComponentProvider.getLabel("", SPUILabelDefinitions.SP_LABEL_SIMPLE);
        gatewayTokenkeyLabel.setId("gatewaysecuritytokenkey");
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

    /**
     * @return
     */
    private void keyNameChanged() {
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
        return getSystemManagement().getConfigurationValue(
                TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_NAME, String.class).getValue();
    }

    private String getSecurityTokenKey() {
        return getSystemManagement().getConfigurationValue(
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
            getSystemManagement().addOrUpdateConfiguration(new TenantConfiguration(
                    TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED.getKeyName(),
                    String.valueOf(configurationEnabled)));
        }

        if (keyNameChanged) {
            getSystemManagement().addOrUpdateConfiguration(new TenantConfiguration(
                    TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_NAME.getKeyName(),
                    gatewayTokenNameTextField.getValue()));
        }
        if (keyChanged) {
            getSystemManagement().addOrUpdateConfiguration(new TenantConfiguration(
                    TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY.getKeyName(),
                    gatewayTokenkeyLabel.getValue()));
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
