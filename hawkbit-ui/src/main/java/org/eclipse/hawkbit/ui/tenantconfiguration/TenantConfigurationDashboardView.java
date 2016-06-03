/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.ui.HawkbitUI;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.tenantconfiguration.ConfigurationItem.ConfigurationItemChangeListener;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Link;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

/**
 * Main UI for the system configuration view.
 */
@SpringView(name = TenantConfigurationDashboardView.VIEW_NAME, ui = HawkbitUI.class)
@ViewScope
public class TenantConfigurationDashboardView extends CustomComponent implements View, ConfigurationItemChangeListener {

    public static final String VIEW_NAME = "spSystemConfig";
    private static final long serialVersionUID = 1L;

    @Autowired
    private DefaultDistributionSetTypeLayout defaultDistributionSetTypeLayout;

    @Autowired
    private AuthenticationConfigurationView authenticationConfigurationView;

    @Autowired
    private PollingConfigurationView pollingConfigurationView;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient UiProperties uiProperties;

    @Autowired
    private transient UINotification uINotification;

    private Button saveConfigurationBtn;
    private Button undoConfigurationBtn;

    private final List<ConfigurationGroup> configurationViews = new ArrayList<>();

    /**
     * Init method adds all Configuration Views to the list of Views.
     */
    @PostConstruct
    public void init() {
        configurationViews.add(defaultDistributionSetTypeLayout);
        configurationViews.add(authenticationConfigurationView);
        configurationViews.add(pollingConfigurationView);
    }

    @Override
    public void enter(final ViewChangeEvent event) {

        final Panel rootPanel = new Panel();
        rootPanel.setStyleName("tenantconfig-root");

        final VerticalLayout rootLayout = new VerticalLayout();
        rootLayout.setSizeFull();
        rootLayout.setMargin(true);
        rootLayout.setSpacing(true);

        configurationViews.forEach(view -> rootLayout.addComponent(view));

        final HorizontalLayout buttonContent = saveConfigurationButtonsLayout();
        rootLayout.addComponent(buttonContent);
        rootLayout.setComponentAlignment(buttonContent, Alignment.BOTTOM_LEFT);
        rootPanel.setContent(rootLayout);
        setCompositionRoot(rootPanel);

        configurationViews.forEach(view -> view.addChangeListener(this));
    }

    private HorizontalLayout saveConfigurationButtonsLayout() {

        final HorizontalLayout hlayout = new HorizontalLayout();
        hlayout.setSpacing(true);
        saveConfigurationBtn = SPUIComponentProvider.getButton(SPUIComponentIdProvider.SYSTEM_CONFIGURATION_SAVE, "", "",
                "", true, FontAwesome.SAVE, SPUIButtonStyleSmallNoBorder.class);
        saveConfigurationBtn.setEnabled(false);
        saveConfigurationBtn.setDescription(i18n.get("configuration.savebutton.tooltip"));
        saveConfigurationBtn.addClickListener(event -> saveConfiguration());
        hlayout.addComponent(saveConfigurationBtn);

        undoConfigurationBtn = SPUIComponentProvider.getButton(SPUIComponentIdProvider.SYSTEM_CONFIGURATION_CANCEL, "",
                "", "", true, FontAwesome.UNDO, SPUIButtonStyleSmallNoBorder.class);
        undoConfigurationBtn.setEnabled(false);
        undoConfigurationBtn.setDescription(i18n.get("configuration.cancellbutton.tooltip"));
        undoConfigurationBtn.addClickListener(event -> undoConfiguration());
        hlayout.addComponent(undoConfigurationBtn);

        final Link linkToSystemConfigHelp = SPUIComponentProvider
                .getHelpLink(uiProperties.getLinks().getDocumentation().getSystemConfigurationView());
        hlayout.addComponent(linkToSystemConfigHelp);

        return hlayout;
    }

    private void saveConfiguration() {

        final boolean isUserInputValid = configurationViews.stream().allMatch(confView -> confView.isUserInputValid());

        if (!isUserInputValid) {
            uINotification.displayValidationError(i18n.get("notification.configuration.save.notpossible"));
            return;
        }
        configurationViews.forEach(confView -> confView.save());

        // More methods
        saveConfigurationBtn.setEnabled(false);
        undoConfigurationBtn.setEnabled(false);
        uINotification.displaySuccess(i18n.get("notification.configuration.save.successful"));
    }

    private void undoConfiguration() {
        configurationViews.forEach(confView -> confView.undo());
        // More methods
        saveConfigurationBtn.setEnabled(false);
        undoConfigurationBtn.setEnabled(false);
    }

    @Override
    public void configurationHasChanged() {
        saveConfigurationBtn.setEnabled(true);
        undoConfigurationBtn.setEnabled(true);
    }

}
