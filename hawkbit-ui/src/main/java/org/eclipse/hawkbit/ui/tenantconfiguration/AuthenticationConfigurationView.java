/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.SecurityTokenGenerator;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.builder.FormComponentBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySystemConfigAuthentication;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.tenantconfiguration.authentication.AnonymousDownloadAuthenticationConfigurationItem;
import org.eclipse.hawkbit.ui.tenantconfiguration.authentication.CertificateAuthenticationConfigurationItem;
import org.eclipse.hawkbit.ui.tenantconfiguration.authentication.GatewaySecurityTokenAuthenticationConfigurationItem;
import org.eclipse.hawkbit.ui.tenantconfiguration.authentication.TargetSecurityTokenAuthenticationConfigurationItem;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import org.springframework.util.StringUtils;

/**
 * View to configure the authentication mode.
 */
public class AuthenticationConfigurationView extends BaseConfigurationView<ProxySystemConfigAuthentication> {

    private static final String DIST_CHECKBOX_STYLE = "dist-checkbox-style";

    private static final long serialVersionUID = 1L;

    private final VaadinMessageSource i18n;

    private final transient SecurityTokenGenerator securityTokenGenerator;
    private CertificateAuthenticationConfigurationItem certificateAuthenticationConfigurationItem;
    private TargetSecurityTokenAuthenticationConfigurationItem targetSecurityTokenAuthenticationConfigurationItem;
    private GatewaySecurityTokenAuthenticationConfigurationItem gatewaySecurityTokenAuthenticationConfigurationItem;
    private AnonymousDownloadAuthenticationConfigurationItem anonymousDownloadAuthenticationConfigurationItem;

    private final UiProperties uiProperties;

    public AuthenticationConfigurationView(final VaadinMessageSource i18n, final UiProperties uiProperties,
            final TenantConfigurationManagement tenantConfigurationManagement,
            final SecurityTokenGenerator securityTokenGenerator) {
        super(tenantConfigurationManagement);
        this.i18n = i18n;
        this.uiProperties = uiProperties;
        this.securityTokenGenerator = securityTokenGenerator;
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        this.targetSecurityTokenAuthenticationConfigurationItem = new TargetSecurityTokenAuthenticationConfigurationItem(
                i18n);
        this.certificateAuthenticationConfigurationItem = new CertificateAuthenticationConfigurationItem(i18n,
                getBinder());
        this.gatewaySecurityTokenAuthenticationConfigurationItem = new GatewaySecurityTokenAuthenticationConfigurationItem(
                i18n, securityTokenGenerator, getBinder());
        this.anonymousDownloadAuthenticationConfigurationItem = new AnonymousDownloadAuthenticationConfigurationItem(
                i18n);
        init();
    }

    private void init() {

        final Panel rootPanel = new Panel();
        rootPanel.setSizeFull();

        rootPanel.addStyleName("config-panel");

        final VerticalLayout vLayout = new VerticalLayout();
        vLayout.setSpacing(false);
        vLayout.setMargin(true);
        vLayout.setSizeFull();

        final Label header = new Label(i18n.getMessage("configuration.authentication.title"));
        header.addStyleName("config-panel-header");
        vLayout.addComponent(header);

        final GridLayout gridLayout = new GridLayout(3, 4);
        gridLayout.setSpacing(true);

        gridLayout.setSizeFull();
        gridLayout.setColumnExpandRatio(1, 1.0F);

        initCertificateAuthConfiguration(gridLayout, 0);
        initTargetTokenConfiguration(gridLayout, 1);
        initGatewayTokenConfiguration(gridLayout, 2);
        initAnonymousDownloadConfiguration(gridLayout, 3);

        vLayout.addComponent(gridLayout);
        rootPanel.setContent(vLayout);
        setCompositionRoot(rootPanel);
    }

    protected void initCertificateAuthConfiguration(GridLayout gridLayout, int row) {
        final CheckBox certificateAuthCheckbox = FormComponentBuilder.getCheckBox(
                UIComponentIdProvider.CERT_AUTH_ALLOWED_CHECKBOX, getBinder(),
                ProxySystemConfigAuthentication::isCertificateAuth,
                ProxySystemConfigAuthentication::setCertificateAuth);
        certificateAuthCheckbox.setStyleName(DIST_CHECKBOX_STYLE);
        certificateAuthCheckbox.addValueChangeListener(valueChangeEvent -> {
            if (valueChangeEvent.getValue()) {
                certificateAuthenticationConfigurationItem.showDetails();
            } else {
                certificateAuthenticationConfigurationItem.hideDetails();
            }
        });
        gridLayout.addComponent(certificateAuthCheckbox, 0, row);
        gridLayout.addComponent(certificateAuthenticationConfigurationItem, 1, row);
    }

    protected void initTargetTokenConfiguration(GridLayout gridLayout, int row) {
        final CheckBox targetSecTokenCheckBox = FormComponentBuilder.getCheckBox(
                UIComponentIdProvider.TARGET_SEC_TOKEN_ALLOWED_CHECKBOX, getBinder(),
                ProxySystemConfigAuthentication::isTargetSecToken, ProxySystemConfigAuthentication::setTargetSecToken);
        targetSecTokenCheckBox.setStyleName(DIST_CHECKBOX_STYLE);
        gridLayout.addComponent(targetSecTokenCheckBox, 0, row);
        gridLayout.addComponent(targetSecurityTokenAuthenticationConfigurationItem, 1, row);
    }

    protected void initGatewayTokenConfiguration(GridLayout gridLayout, int row) {
        final CheckBox gatewaySecTokenCheckBox = FormComponentBuilder.getCheckBox(
                UIComponentIdProvider.GATEWAY_SEC_TOKEN_ALLOWED_CHECKBOX, getBinder(),
                ProxySystemConfigAuthentication::isGatewaySecToken,
                ProxySystemConfigAuthentication::setGatewaySecToken);
        gatewaySecTokenCheckBox.setStyleName(DIST_CHECKBOX_STYLE);
        gatewaySecTokenCheckBox.addValueChangeListener(valueChangeEvent -> {
            if (valueChangeEvent.getValue()) {
                gatewaySecurityTokenAuthenticationConfigurationItem.showDetails();
            } else {
                gatewaySecurityTokenAuthenticationConfigurationItem.hideDetails();
            }
        });
        gridLayout.addComponent(gatewaySecTokenCheckBox, 0, row);
        gridLayout.addComponent(gatewaySecurityTokenAuthenticationConfigurationItem, 1, row);
    }

    protected void initAnonymousDownloadConfiguration(GridLayout gridLayout, int row) {
        final CheckBox downloadAnonymousCheckBox = FormComponentBuilder.getCheckBox(
                UIComponentIdProvider.DOWNLOAD_ANONYMOUS_CHECKBOX, getBinder(),
                ProxySystemConfigAuthentication::isDownloadAnonymous,
                ProxySystemConfigAuthentication::setDownloadAnonymous);
        downloadAnonymousCheckBox.setStyleName(DIST_CHECKBOX_STYLE);
        gridLayout.addComponent(downloadAnonymousCheckBox, 0, row);
        gridLayout.addComponent(anonymousDownloadAuthenticationConfigurationItem, 1, row);

        final Link linkToSecurityHelp = SPUIComponentProvider.getHelpLink(i18n,
                uiProperties.getLinks().getDocumentation().getSecurity());
        gridLayout.addComponent(linkToSecurityHelp, 2, row);
        gridLayout.setComponentAlignment(linkToSecurityHelp, Alignment.BOTTOM_RIGHT);
    }

    @Override
    public void save() {
        writeConfigOption(TenantConfigurationKey.AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED,
                getBinderBean().isTargetSecToken());
        writeConfigOption(TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED,
                getBinderBean().isGatewaySecToken());
        writeConfigOption(TenantConfigurationKey.ANONYMOUS_DOWNLOAD_MODE_ENABLED,
                getBinderBean().isDownloadAnonymous());

        if (getBinderBean().isGatewaySecToken()) {
            writeConfigOption(TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY,
                    getBinderBean().getGatewaySecurityToken());
        }

        writeConfigOption(TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_ENABLED,
                getBinderBean().isCertificateAuth());
        if (getBinderBean().isCertificateAuth()) {
            final String value = getBinderBean().getCaRootAuthority() != null ? getBinderBean().getCaRootAuthority()
                    : "";
            writeConfigOption(TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_AUTHORITY_NAME, value);
        }
    }

    @Override
    protected ProxySystemConfigAuthentication populateSystemConfig() {
        final ProxySystemConfigAuthentication configBean = new ProxySystemConfigAuthentication();
        configBean.setCertificateAuth(readConfigOption(TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_ENABLED));
        configBean.setTargetSecToken(
                readConfigOption(TenantConfigurationKey.AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED));
        configBean.setGatewaySecToken(
                readConfigOption(TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED));
        configBean.setDownloadAnonymous(readConfigOption(TenantConfigurationKey.ANONYMOUS_DOWNLOAD_MODE_ENABLED));

        String securityToken = getTenantConfigurationManagement().getConfigurationValue(
                TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY, String.class).getValue();
        if (StringUtils.isEmpty(securityToken)) {
            securityToken = securityTokenGenerator.generateToken();
        }
        configBean.setGatewaySecurityToken(securityToken);
        configBean.setCaRootAuthority(getCaRootAuthorityValue());

        return configBean;
    }

    private String getCaRootAuthorityValue() {
        return getTenantConfigurationManagement()
                .getConfigurationValue(TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_AUTHORITY_NAME, String.class)
                .getValue();
    }

}
