/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.miscs;

import java.util.function.Consumer;

import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.builder.BoundComponent;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyAssignmentWindow;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.data.Binder;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Link;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 * Layout for target to distribution set assignment
 */
public class AssignmentWindowLayout extends VerticalLayout {
    private static final long serialVersionUID = 1L;

    private final Binder<ProxyAssignmentWindow> proxyAssignmentBinder;
    private final transient AssignmentWindowLayoutComponentBuilder componentBuilder;

    private final BoundComponent<ActionTypeOptionGroupAssignmentLayout> actionTypeLayout;
    private final CheckBox maintenanceWindowToggle;
    private final CheckBox confirmationRequiredToggle;
    
    private final Link confirmationHelpLink;

    private final BoundComponent<TextField> maintenanceSchedule;
    private final BoundComponent<TextField> maintenanceDuration;
    private final ComboBox<String> maintenanceTimeZoneCombo;

    private final MaintenanceWindowLayout maintenanceWindowLayout;
    private final Link maintenanceHelpLink;

    /**
     * Constructor for AssignmentWindowLayout
     *
     * @param i18n
     *            VaadinMessageSource
     * @param uiProperties
     *            UiProperties
     */
    public AssignmentWindowLayout(final VaadinMessageSource i18n, final UiProperties uiProperties) {
        this.proxyAssignmentBinder = new Binder<>();
        this.componentBuilder = new AssignmentWindowLayoutComponentBuilder(i18n);

        this.actionTypeLayout = componentBuilder.createActionTypeOptionGroupLayout(proxyAssignmentBinder);
        this.maintenanceWindowToggle = componentBuilder.createEnableMaintenanceWindowToggle(proxyAssignmentBinder);
        this.confirmationRequiredToggle = componentBuilder.createConfirmationToggle(proxyAssignmentBinder);
        this.confirmationHelpLink = componentBuilder.createConfirmationHelpLink(uiProperties);

        this.maintenanceSchedule = componentBuilder.createMaintenanceSchedule(proxyAssignmentBinder);
        this.maintenanceDuration = componentBuilder.createMaintenanceDuration(proxyAssignmentBinder);
        this.maintenanceTimeZoneCombo = componentBuilder.createMaintenanceTimeZoneCombo(proxyAssignmentBinder);
        this.maintenanceSchedule.setRequired(false);
        this.maintenanceDuration.setRequired(false);
        this.maintenanceWindowLayout = new MaintenanceWindowLayout(i18n, maintenanceSchedule.getComponent(),
                maintenanceDuration.getComponent(), maintenanceTimeZoneCombo,
                componentBuilder.createMaintenanceScheduleTranslator());
        this.maintenanceHelpLink = componentBuilder.createMaintenanceHelpLink(uiProperties);

        initLayout();
        buildLayout();
        addValueChangeListeners();
    }

    private void initLayout() {
        setSizeFull();
        setMargin(false);
        setSpacing(false);
    }

    private void buildLayout() {
        addComponent(actionTypeLayout.getComponent());

        final HorizontalLayout maintenanceWindowToggleLayout = new HorizontalLayout();
        maintenanceWindowToggleLayout.addComponent(maintenanceWindowToggle);
        maintenanceWindowToggleLayout.addComponent(maintenanceHelpLink);
        addComponent(maintenanceWindowToggleLayout);

        maintenanceWindowLayout.setVisible(false);
        maintenanceWindowLayout.setEnabled(false);
        addComponent(maintenanceWindowLayout);

        final HorizontalLayout confirmationOptionsLayout = new HorizontalLayout();
        confirmationOptionsLayout.addComponent(confirmationRequiredToggle);
        confirmationOptionsLayout.addComponent(confirmationHelpLink);
        addComponent(confirmationOptionsLayout);
        refreshConfirmCheckBoxState(false);
    }

    public void refreshConfirmCheckBoxState(final boolean confirmationFlowEnabled) {
        confirmationRequiredToggle.setEnabled(confirmationFlowEnabled);
        confirmationRequiredToggle.setVisible(confirmationFlowEnabled);
        confirmationHelpLink.setEnabled(confirmationFlowEnabled);
        confirmationHelpLink.setVisible(confirmationFlowEnabled);
    }

    private void addValueChangeListeners() {

        maintenanceWindowToggle.addValueChangeListener(event -> {
            final boolean isMaintenanceWindowEnabled = event.getValue();
            maintenanceSchedule.setRequired(isMaintenanceWindowEnabled);
            maintenanceDuration.setRequired(isMaintenanceWindowEnabled);
            clearMaintenanceFields();
            maintenanceWindowLayout.setVisible(isMaintenanceWindowEnabled);
            maintenanceWindowLayout.setEnabled(isMaintenanceWindowEnabled);
        });

        actionTypeLayout.getComponent().getActionTypeOptionGroup().addValueChangeListener(event -> {
            actionTypeLayout.setRequired(event.getValue() == ActionType.TIMEFORCED);

            if (event.getValue() == ActionType.DOWNLOAD_ONLY) {
                maintenanceWindowToggle.setValue(false);
                maintenanceWindowToggle.setEnabled(false);
                maintenanceHelpLink.setEnabled(false);
            } else {
                maintenanceWindowToggle.setEnabled(true);
                maintenanceHelpLink.setEnabled(true);
            }
        });
    }

    private void clearMaintenanceFields() {
        maintenanceSchedule.getComponent().setValue("");
        maintenanceDuration.getComponent().setValue("");
        maintenanceTimeZoneCombo.setValue(SPDateTimeUtil.getClientTimeZoneOffsetId());
    }

    /**
     * @return Proxy assignment binder
     */
    public Binder<ProxyAssignmentWindow> getProxyAssignmentBinder() {
        return proxyAssignmentBinder;
    }

    /**
     * Add validation listener
     *
     * @param validationCallback
     *          validation call back event
     */
    public void addValidationListener(final Consumer<Boolean> validationCallback) {
        proxyAssignmentBinder.addStatusChangeListener(event -> validationCallback.accept(event.getBinder().isValid()));
    }
}
