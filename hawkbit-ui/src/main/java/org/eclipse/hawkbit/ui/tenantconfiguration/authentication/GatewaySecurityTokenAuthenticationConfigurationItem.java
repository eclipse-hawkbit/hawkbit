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
import org.eclipse.hawkbit.security.SecurityTokenGenerator;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmall;
import org.eclipse.hawkbit.ui.tenantconfiguration.generic.AbstractBooleanTenantConfigurationItem;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * This class represents the UI item for the gateway security token section in
 * the authentication configuration view.
 */
public class GatewaySecurityTokenAuthenticationConfigurationItem extends AbstractBooleanTenantConfigurationItem {

    private static final long serialVersionUID = 1L;

    private final transient SecurityTokenGenerator securityTokenGenerator;

    private final Label gatewayTokenkeyLabel;

    private boolean configurationEnabled;
    private boolean configurationEnabledChange;

    private boolean keyChanged;

    private final VerticalLayout detailLayout;

    public GatewaySecurityTokenAuthenticationConfigurationItem(
            final TenantConfigurationManagement tenantConfigurationManagement, final VaadinMessageSource i18n,
            final SecurityTokenGenerator securityTokenGenerator) {
        super(TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED, tenantConfigurationManagement,
                i18n);
        this.securityTokenGenerator = securityTokenGenerator;

        super.init("label.configuration.auth.gatewaytoken");

        configurationEnabled = isConfigEnabled();

        detailLayout = new VerticalLayout();
        detailLayout.setImmediate(true);

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

        keyGenerationLayout.addComponent(gatewayTokenkeyLabel);
        keyGenerationLayout.addComponent(gatewaytokenBtn);

        detailLayout.addComponent(keyGenerationLayout);

        if (isConfigEnabled()) {
            gatewayTokenkeyLabel.setValue(getSecurityTokenKey());
            setDetailVisible(true);
        }
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
        if (StringUtils.isEmpty(gatewayTokenKey)) {
            gatewayTokenKey = securityTokenGenerator.generateToken();
            keyChanged = true;
        }
        gatewayTokenkeyLabel.setValue(gatewayTokenKey);
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

        if (keyChanged) {
            getTenantConfigurationManagement().addOrUpdateConfiguration(
                    TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY,
                    gatewayTokenkeyLabel.getValue());
        }
    }

    @Override
    public void undo() {
        configurationEnabledChange = false;
        keyChanged = false;
        gatewayTokenkeyLabel.setValue(getSecurityTokenKey());
    }

}
