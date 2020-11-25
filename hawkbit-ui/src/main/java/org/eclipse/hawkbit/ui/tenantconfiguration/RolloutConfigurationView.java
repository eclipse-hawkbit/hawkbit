/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.builder.FormComponentBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySystemConfigRollout;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.tenantconfiguration.rollout.ApprovalConfigurationItem;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

/**
 * Provides configuration of the RolloutManagement including enabling/disabling
 * of the approval workflow.
 */
public class RolloutConfigurationView extends BaseConfigurationView<ProxySystemConfigRollout> {

    private static final long serialVersionUID = 1L;

    private final VaadinMessageSource i18n;
    private final UiProperties uiProperties;
    private final ApprovalConfigurationItem approvalConfigurationItem;

    RolloutConfigurationView(final VaadinMessageSource i18n, final UiProperties uiProperties,
            final TenantConfigurationManagement tenantConfigurationManagement) {
        super(tenantConfigurationManagement);
        this.i18n = i18n;
        this.uiProperties = uiProperties;
        this.approvalConfigurationItem = new ApprovalConfigurationItem(i18n);
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
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

        final Label header = new Label(i18n.getMessage("configuration.rollout.title"));
        header.addStyleName("config-panel-header");
        vLayout.addComponent(header);

        final GridLayout gridLayout = new GridLayout(3, 1);
        gridLayout.setSpacing(true);
        gridLayout.setColumnExpandRatio(1, 1.0F);
        gridLayout.setSizeFull();

        final CheckBox approvalCheckbox = FormComponentBuilder.getCheckBox(
                UIComponentIdProvider.ROLLOUT_APPROVAL_ENABLED_CHECKBOX, getBinder(),
                ProxySystemConfigRollout::isRolloutApproval, ProxySystemConfigRollout::setRolloutApproval);

        gridLayout.addComponent(approvalCheckbox, 0, 0);
        gridLayout.addComponent(approvalConfigurationItem, 1, 0);
        final Link linkToApprovalHelp = SPUIComponentProvider.getHelpLink(i18n,
                uiProperties.getLinks().getDocumentation().getRollout());
        gridLayout.addComponent(linkToApprovalHelp, 2, 0);
        gridLayout.setComponentAlignment(linkToApprovalHelp, Alignment.BOTTOM_RIGHT);

        vLayout.addComponent(gridLayout);
        rootPanel.setContent(vLayout);
        setCompositionRoot(rootPanel);
    }

    @Override
    protected ProxySystemConfigRollout populateSystemConfig() {
        ProxySystemConfigRollout configBean = new ProxySystemConfigRollout();
        configBean.setRolloutApproval(readConfigOption(TenantConfigurationKey.ROLLOUT_APPROVAL_ENABLED));
        return configBean;
    }

    @Override
    public void save() {
        writeConfigOption(TenantConfigurationKey.ROLLOUT_APPROVAL_ENABLED, getBinderBean().isRolloutApproval());
    }

}
