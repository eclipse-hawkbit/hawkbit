/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration.authentication;

import org.eclipse.hawkbit.security.SecurityTokenGenerator;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySystemConfigAuthentication;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmall;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.data.Binder;
import com.vaadin.data.ReadOnlyHasValue;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * This class represents the UI item for the gateway security token section in
 * the authentication configuration view.
 */
public class GatewaySecurityTokenAuthenticationConfigurationItem extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private final transient SecurityTokenGenerator securityTokenGenerator;
    private final VerticalLayout detailLayout;
    private final Binder<ProxySystemConfigAuthentication> binder;

    /**
     * Constructor for GatewaySecurityTokenAuthenticationConfigurationItem
     *
     * @param i18n
     *            VaadinMessageSource
     * @param securityTokenGenerator
     *            SecurityTokenGenerator
     * @param binder
     *            System config window binder
     */
    public GatewaySecurityTokenAuthenticationConfigurationItem(final VaadinMessageSource i18n,
            final SecurityTokenGenerator securityTokenGenerator, final Binder<ProxySystemConfigAuthentication> binder) {
        this.securityTokenGenerator = securityTokenGenerator;
        this.binder = binder;
        this.setSpacing(false);
        this.setMargin(false);
        addComponent(SPUIComponentProvider.generateLabel(i18n, "label.configuration.auth.gatewaytoken"));

        detailLayout = new VerticalLayout();
        detailLayout.setMargin(false);
        detailLayout.setSpacing(false);

        final Button gatewaytokenBtn = SPUIComponentProvider.getButton(null,
                i18n.getMessage("configuration.button.regenerateKey"), "", ValoTheme.BUTTON_TINY + " " + "redicon",
                true, null, SPUIButtonStyleSmall.class);

        gatewaytokenBtn.setIcon(VaadinIcons.REFRESH);
        gatewaytokenBtn.addClickListener(event -> refreshGatewayToken());

        final Label gatewayTokenLabel = new LabelBuilder().id("gatewaysecuritytokenkey").name("").buildLabel();
        gatewayTokenLabel.addStyleName("gateway-token-label");
        final ReadOnlyHasValue<String> gatewayTokenFieldBindable = new ReadOnlyHasValue<>(gatewayTokenLabel::setValue);
        binder.bind(gatewayTokenFieldBindable, ProxySystemConfigAuthentication::getGatewaySecurityToken, null);

        final HorizontalLayout keyGenerationLayout = new HorizontalLayout();
        keyGenerationLayout.setSpacing(true);
        keyGenerationLayout.addComponent(gatewayTokenLabel);
        keyGenerationLayout.addComponent(gatewaytokenBtn);
        detailLayout.addComponent(keyGenerationLayout);
        if (binder.getBean().isGatewaySecToken()) {
            showDetails();
        }
    }

    /**
     * Show gateway token detail
     */
    public void showDetails() {
        addComponent(detailLayout);
    }

    /**
     * Hide gateway token detail
     */
    public void hideDetails() {
        removeComponent(detailLayout);
    }

    private void refreshGatewayToken() {
        binder.getBean().setGatewaySecurityToken(securityTokenGenerator.generateToken());
        binder.setBean(binder.getBean());
    }
}
