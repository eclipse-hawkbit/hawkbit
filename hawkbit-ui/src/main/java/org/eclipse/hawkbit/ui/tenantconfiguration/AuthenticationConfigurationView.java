/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.tenantconfiguration.authentication.CertificateAuthenticationConfigurationItem;
import org.eclipse.hawkbit.ui.tenantconfiguration.authentication.GatewaySecurityTokenAuthenticationConfigurationItem;
import org.eclipse.hawkbit.ui.tenantconfiguration.authentication.TargetSecurityTokenAuthenticationConfigurationItem;
import org.eclipse.hawkbit.ui.tenantconfiguration.authentication.TenantConfigurationItem;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

/**
 * View to configure the authentication mode.
 *
 *
 *
 */
@SpringComponent
@ViewScope
public class AuthenticationConfigurationView extends BaseConfigurationView
        implements ConfigurationGroup, TenantConfigurationItem.TenantConfigurationChangeListener, ValueChangeListener {

    /**
    *
    */
    private static final String DIST_CHECKBOX_STYLE = "dist-checkbox-style";

    /**
    *
    */
    private static final long serialVersionUID = 1L;

    @Autowired
    private I18N i18n;

    @Autowired
    private CertificateAuthenticationConfigurationItem certificateAuthenticationConfigurationItem;

    @Autowired
    private TargetSecurityTokenAuthenticationConfigurationItem targetSecurityTokenAuthenticationConfigurationItem;

    @Autowired
    private GatewaySecurityTokenAuthenticationConfigurationItem gatewaySecurityTokenAuthenticationConfigurationItem;

    private CheckBox gatewaySecTokenCheckBox;

    private CheckBox targetSecTokenCheckBox;

    private CheckBox certificateAuthCheckbox;

    /**
     * Initialize Authentication Configuration layout.
     */
    @PostConstruct
    public void init() {

        final Panel rootPanel = new Panel();
        rootPanel.setSizeFull();

        rootPanel.addStyleName("config-panel");

        final VerticalLayout vLayout = new VerticalLayout();
        vLayout.setMargin(true);
        vLayout.setSizeFull();

        final Label headerDisSetType = new Label(i18n.get("configuration.authentication.title"));
        headerDisSetType.addStyleName("config-panel-header");
        vLayout.addComponent(headerDisSetType);

        final GridLayout gridLayout = new GridLayout(2, 3);
        gridLayout.setSpacing(true);
        gridLayout.setImmediate(true);
        gridLayout.setColumnExpandRatio(1, 1.0F);
        gridLayout.setSizeFull();

        certificateAuthCheckbox = SPUIComponentProvider.getCheckBox("", DIST_CHECKBOX_STYLE, null, false, "");
        certificateAuthCheckbox.setValue(certificateAuthenticationConfigurationItem.isConfigEnabled());
        certificateAuthCheckbox.addValueChangeListener(this);
        certificateAuthenticationConfigurationItem.addConfigurationChangeListener(this);
        gridLayout.addComponent(certificateAuthCheckbox, 0, 0);
        gridLayout.addComponent(certificateAuthenticationConfigurationItem, 1, 0);

        targetSecTokenCheckBox = SPUIComponentProvider.getCheckBox("", DIST_CHECKBOX_STYLE, null, false, "");
        targetSecTokenCheckBox.setValue(targetSecurityTokenAuthenticationConfigurationItem.isConfigEnabled());
        targetSecTokenCheckBox.addValueChangeListener(this);
        targetSecurityTokenAuthenticationConfigurationItem.addConfigurationChangeListener(this);
        gridLayout.addComponent(targetSecTokenCheckBox, 0, 1);
        gridLayout.addComponent(targetSecurityTokenAuthenticationConfigurationItem, 1, 1);

        gatewaySecTokenCheckBox = SPUIComponentProvider.getCheckBox("", DIST_CHECKBOX_STYLE, null, false, "");
        gatewaySecTokenCheckBox.setId("gatewaysecuritycheckbox");
        gatewaySecTokenCheckBox.setValue(gatewaySecurityTokenAuthenticationConfigurationItem.isConfigEnabled());
        gatewaySecTokenCheckBox.addValueChangeListener(this);
        gatewaySecurityTokenAuthenticationConfigurationItem.addConfigurationChangeListener(this);
        gridLayout.addComponent(gatewaySecTokenCheckBox, 0, 2);
        gridLayout.addComponent(gatewaySecurityTokenAuthenticationConfigurationItem, 1, 2);

        vLayout.addComponent(gridLayout);
        rootPanel.setContent(vLayout);
        setCompositionRoot(rootPanel);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.hawkbit.server.ui.tenantconfiguration.ConfigurationGroup#save
     * ()
     */
    @Override
    public void save() {
        certificateAuthenticationConfigurationItem.save();
        targetSecurityTokenAuthenticationConfigurationItem.save();
        gatewaySecurityTokenAuthenticationConfigurationItem.save();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.hawkbit.server.ui.tenantconfiguration.ConfigurationGroup#undo
     * ()
     */
    @Override
    public void undo() {
        certificateAuthenticationConfigurationItem.undo();
        targetSecurityTokenAuthenticationConfigurationItem.undo();
        gatewaySecurityTokenAuthenticationConfigurationItem.undo();
        certificateAuthCheckbox.setValue(certificateAuthenticationConfigurationItem.isConfigEnabled());
        targetSecTokenCheckBox.setValue(targetSecurityTokenAuthenticationConfigurationItem.isConfigEnabled());
        gatewaySecTokenCheckBox.setValue(gatewaySecurityTokenAuthenticationConfigurationItem.isConfigEnabled());
    }

    @Override
    public void configurationHasChanged() {
        notifyConfigurationChanged();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.data.Property.ValueChangeListener#valueChange(com.vaadin.
     * data.Property. ValueChangeEvent)
     */
    @Override
    public void valueChange(final ValueChangeEvent event) {

        if (event.getProperty() instanceof CheckBox) {
            notifyConfigurationChanged();

            CheckBox checkBox = (CheckBox) event.getProperty();
            TenantConfigurationItem configurationItem = null;

            if (checkBox == gatewaySecTokenCheckBox) {
                configurationItem = gatewaySecurityTokenAuthenticationConfigurationItem;
            } else if (checkBox == targetSecTokenCheckBox) {
                configurationItem = targetSecurityTokenAuthenticationConfigurationItem;
            } else if (checkBox == certificateAuthCheckbox) {
                configurationItem = certificateAuthenticationConfigurationItem;
            } else {
                return;
            }

            if (checkBox.getValue()) {
                configurationItem.configEnable();
            } else {
                configurationItem.configDisable();
            }

        }
    }
}
