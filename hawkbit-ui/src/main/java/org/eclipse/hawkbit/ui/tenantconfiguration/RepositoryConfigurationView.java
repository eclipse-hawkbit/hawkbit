/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration;

import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.builder.FormComponentBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySystemConfigWindow;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.tenantconfiguration.repository.ActionAutoCleanupConfigurationItem;
import org.eclipse.hawkbit.ui.tenantconfiguration.repository.ActionAutoCloseConfigurationItem;
import org.eclipse.hawkbit.ui.tenantconfiguration.repository.MultiAssignmentsConfigurationItem;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.data.Binder;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

/**
 * View to configure the authentication mode.
 */
public class RepositoryConfigurationView extends CustomComponent {

    private static final String DIST_CHECKBOX_STYLE = "dist-checkbox-style";

    private static final long serialVersionUID = 1L;

    private final VaadinMessageSource i18n;

    private final UiProperties uiProperties;

    private final ActionAutoCloseConfigurationItem actionAutocloseConfigurationItem;

    private final ActionAutoCleanupConfigurationItem actionAutocleanupConfigurationItem;

    private final MultiAssignmentsConfigurationItem multiAssignmentsConfigurationItem;

    private CheckBox multiAssignmentsCheckBox;

    private final Binder<ProxySystemConfigWindow> binder;

    RepositoryConfigurationView(final VaadinMessageSource i18n, final UiProperties uiProperties,
            final Binder<ProxySystemConfigWindow> binder) {
        this.i18n = i18n;
        this.uiProperties = uiProperties;
        this.actionAutocloseConfigurationItem = new ActionAutoCloseConfigurationItem(i18n);
        this.actionAutocleanupConfigurationItem = new ActionAutoCleanupConfigurationItem(binder, i18n);
        this.multiAssignmentsConfigurationItem = new MultiAssignmentsConfigurationItem(i18n, binder);
        this.binder = binder;

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

        final Label header = new Label(i18n.getMessage("configuration.repository.title"));
        header.addStyleName("config-panel-header");
        vLayout.addComponent(header);

        final GridLayout gridLayout = new GridLayout(3, 3);
        gridLayout.setSpacing(true);

        gridLayout.setColumnExpandRatio(1, 1.0F);
        gridLayout.setSizeFull();

        final CheckBox actionAutoCloseCheckBox = FormComponentBuilder.getCheckBox(
                UIComponentIdProvider.REPOSITORY_ACTIONS_AUTOCLOSE_CHECKBOX, binder,
                ProxySystemConfigWindow::isActionAutoclose, ProxySystemConfigWindow::setActionAutoclose);
        actionAutoCloseCheckBox.setStyleName(DIST_CHECKBOX_STYLE);
        actionAutoCloseCheckBox.setEnabled(!binder.getBean().isMultiAssignments());
        actionAutocloseConfigurationItem.setEnabled(!binder.getBean().isMultiAssignments());
        gridLayout.addComponent(actionAutoCloseCheckBox, 0, 0);
        gridLayout.addComponent(actionAutocloseConfigurationItem, 1, 0);

        multiAssignmentsCheckBox = FormComponentBuilder.getCheckBox(
                UIComponentIdProvider.REPOSITORY_MULTI_ASSIGNMENTS_CHECKBOX, binder,
                ProxySystemConfigWindow::isMultiAssignments, ProxySystemConfigWindow::setMultiAssignments);
        multiAssignmentsCheckBox.setStyleName(DIST_CHECKBOX_STYLE);
        multiAssignmentsCheckBox.setEnabled(!binder.getBean().isMultiAssignments());
        multiAssignmentsConfigurationItem.setEnabled(!binder.getBean().isMultiAssignments());
        multiAssignmentsCheckBox.addValueChangeListener(event -> {
            actionAutoCloseCheckBox.setEnabled(!event.getValue());
            actionAutocloseConfigurationItem.setEnabled(!event.getValue());
            if (event.getValue()) {
                multiAssignmentsConfigurationItem.showSettings();
            } else {
                multiAssignmentsConfigurationItem.hideSettings();
            }
        });
        gridLayout.addComponent(multiAssignmentsCheckBox, 0, 1);
        gridLayout.addComponent(multiAssignmentsConfigurationItem, 1, 1);

        final CheckBox actionAutoCleanupCheckBox = FormComponentBuilder.getCheckBox(
                UIComponentIdProvider.REPOSITORY_ACTIONS_AUTOCLEANUP_CHECKBOX, binder,
                ProxySystemConfigWindow::isActionAutocleanup, ProxySystemConfigWindow::setActionAutocleanup);
        actionAutoCleanupCheckBox.setStyleName(DIST_CHECKBOX_STYLE);
        actionAutoCleanupCheckBox.addValueChangeListener(event -> {
            if (event.getValue()) {
                actionAutocleanupConfigurationItem.showSettings();
            } else {
                actionAutocleanupConfigurationItem.hideSettings();
            }
        });
        gridLayout.addComponent(actionAutoCleanupCheckBox, 0, 2);
        gridLayout.addComponent(actionAutocleanupConfigurationItem, 1, 2);

        final Link linkToProvisioningHelp = SPUIComponentProvider.getHelpLink(i18n,
                uiProperties.getLinks().getDocumentation().getProvisioningStateMachine());
        gridLayout.addComponent(linkToProvisioningHelp, 2, 2);
        gridLayout.setComponentAlignment(linkToProvisioningHelp, Alignment.BOTTOM_RIGHT);

        vLayout.addComponent(gridLayout);
        rootPanel.setContent(vLayout);
        setCompositionRoot(rootPanel);
    }

    /**
     * Disable multiple assignment option
     */
    public void disableMultipleAssignmentOption() {
        multiAssignmentsCheckBox.setEnabled(false);
        multiAssignmentsConfigurationItem.setEnabled(false);
    }

}
