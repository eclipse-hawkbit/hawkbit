/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.miscs;

import org.eclipse.hawkbit.repository.MaintenanceScheduleHelper;
import org.eclipse.hawkbit.repository.exception.InvalidMaintenanceScheduleException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.builder.BoundComponent;
import org.eclipse.hawkbit.ui.common.builder.FormComponentBuilder;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyAssignmentWindow;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Binder;
import com.vaadin.data.Binder.Binding;
import com.vaadin.data.ValidationResult;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ExternalResource;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

import static org.eclipse.hawkbit.ui.utils.UIMessageIdProvider.CAPTION_ACTION_CONFIRMATION_REQUIRED;

/**
 * Builder for Assignment window components.
 */
public class AssignmentWindowLayoutComponentBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(AssignmentWindowLayoutComponentBuilder.class);

    private final VaadinMessageSource i18n;

    /**
     * Constructor for AssignmentWindowLayoutComponentBuilder
     *
     * @param i18n
     *          VaadinMessageSource
     */
    public AssignmentWindowLayoutComponentBuilder(final VaadinMessageSource i18n) {
        this.i18n = i18n;
    }

    /**
     * create bound {@link ActionTypeOptionGroupAssignmentLayout}
     * 
     * @param binder
     *            binder the input will be bound to
     * @return input component
     */
    public BoundComponent<ActionTypeOptionGroupAssignmentLayout> createActionTypeOptionGroupLayout(
            final Binder<ProxyAssignmentWindow> binder) {
        final BoundComponent<ActionTypeOptionGroupAssignmentLayout> actionTypeGroupBounded = FormComponentBuilder
                .createActionTypeOptionGroupLayout(binder, i18n,
                        UIComponentIdProvider.DEPLOYMENT_ASSIGNMENT_ACTION_TYPE_OPTIONS_ID);
        actionTypeGroupBounded.setRequired(false);
        actionTypeGroupBounded.getComponent().addStyleName("margin-small");

        return actionTypeGroupBounded;
    }

    /**
     * Create toggle for maintenance window
     *
     * @param binder
     *          Proxy assignment window binder
     *
     * @return Maintenance window checkbox
     */
    public CheckBox createEnableMaintenanceWindowToggle(final Binder<ProxyAssignmentWindow> binder) {
        final CheckBox maintenanceWindowToggle = FormComponentBuilder.createCheckBox(
                i18n.getMessage("caption.maintenancewindow.enabled"),
                UIComponentIdProvider.MAINTENANCE_WINDOW_ENABLED_ID, binder,
                ProxyAssignmentWindow::isMaintenanceWindowEnabled, ProxyAssignmentWindow::setMaintenanceWindowEnabled);
        maintenanceWindowToggle.addStyleName(ValoTheme.CHECKBOX_SMALL);
        maintenanceWindowToggle.addStyleName("dist-window-maintenance-window-enable");

        return maintenanceWindowToggle;
    }

    public CheckBox createConfirmationToggle(final Binder<ProxyAssignmentWindow> binder) {
        final CheckBox confirmationToggle = FormComponentBuilder.createCheckBox(
                i18n.getMessage(CAPTION_ACTION_CONFIRMATION_REQUIRED),
                UIComponentIdProvider.ASSIGNMENT_CONFIRMATION_REQUIRED, binder,
                ProxyAssignmentWindow::isConfirmationRequired, ProxyAssignmentWindow::setConfirmationRequired);
        confirmationToggle.addStyleName(ValoTheme.CHECKBOX_SMALL);
        confirmationToggle.addStyleName("dist-window-maintenance-window-enable");

        return confirmationToggle;
    }

    /**
     * Create maintenance schedule
     *
     * @param binder
     *            Proxy assignment window binder
     *
     * @return maintenance schedule text field
     */
    public BoundComponent<TextField> createMaintenanceSchedule(final Binder<ProxyAssignmentWindow> binder) {
        final TextField maintenanceSchedule = new TextFieldBuilder(Action.MAINTENANCE_WINDOW_SCHEDULE_LENGTH)
                .id(UIComponentIdProvider.MAINTENANCE_WINDOW_SCHEDULE_ID)
                .caption(i18n.getMessage("caption.maintenancewindow.schedule")).prompt("0 0 3 ? * 6")
                .buildTextComponent();

        final Binding<ProxyAssignmentWindow, String> binding = binder.forField(maintenanceSchedule)
                .asRequired(i18n.getMessage("message.maintenancewindow.schedule.required.error"))
                .withValidator((cronSchedule, context) -> {
                    try {
                        MaintenanceScheduleHelper.validateCronSchedule(cronSchedule);
                        return ValidationResult.ok();
                    } catch (final InvalidMaintenanceScheduleException e) {
                        LOG.trace("Cron Schedule of Maintenance Window is invalid in UI: {}", e.getMessage());
                        return ValidationResult.error(
                                i18n.getMessage(UIMessageIdProvider.CRON_VALIDATION_ERROR) + ": " + e.getMessage());
                    }
                }).bind(ProxyAssignmentWindow::getMaintenanceSchedule, ProxyAssignmentWindow::setMaintenanceSchedule);

        return new BoundComponent<>(maintenanceSchedule, binding);
    }

    /**
     * Create maintenance duration
     *
     * @param binder
     *          Proxy assignment window binder
     *
     * @return maintenance duration text field
     */
    public BoundComponent<TextField> createMaintenanceDuration(final Binder<ProxyAssignmentWindow> binder) {
        final TextField maintenanceDuration = new TextFieldBuilder(Action.MAINTENANCE_WINDOW_DURATION_LENGTH)
                .id(UIComponentIdProvider.MAINTENANCE_WINDOW_DURATION_ID)
                .caption(i18n.getMessage("caption.maintenancewindow.duration")).prompt("hh:mm:ss").buildTextComponent();

        final Binding<ProxyAssignmentWindow, String> binding = binder.forField(maintenanceDuration)
                .asRequired(i18n.getMessage("message.maintenancewindow.duration.required.error"))
                .withValidator((duration, context) -> {
                    try {
                        MaintenanceScheduleHelper.validateDuration(duration);
                        return ValidationResult.ok();
                    } catch (final InvalidMaintenanceScheduleException e) {
                        LOG.trace("Duration of Maintenance Window is invalid in UI: {}", e.getMessage());
                        return ValidationResult.error(i18n.getMessage(
                                "message.maintenancewindow.duration.validation.error", e.getDurationErrorIndex()));
                    }
                }).bind(ProxyAssignmentWindow::getMaintenanceDuration, ProxyAssignmentWindow::setMaintenanceDuration);

        return new BoundComponent<>(maintenanceDuration, binding);
    }

    /**
     * Create maintenance time zone combo
     *
     * @param binder
     *          Proxy assignment window binder
     *
     * @return Maintenance timezone combobox
     */
    public ComboBox<String> createMaintenanceTimeZoneCombo(final Binder<ProxyAssignmentWindow> binder) {
        final ComboBox<String> maintenanceTimeZoneCombo = new ComboBox<>();

        maintenanceTimeZoneCombo.setId(UIComponentIdProvider.MAINTENANCE_WINDOW_TIME_ZONE_ID);
        maintenanceTimeZoneCombo.setCaption(i18n.getMessage("caption.maintenancewindow.timezone"));
        maintenanceTimeZoneCombo.addStyleName(ValoTheme.COMBOBOX_SMALL);

        maintenanceTimeZoneCombo.setTextInputAllowed(false);
        maintenanceTimeZoneCombo.setEmptySelectionAllowed(false);

        maintenanceTimeZoneCombo.setItems(SPDateTimeUtil.getAllTimeZoneOffsetIds());

        binder.forField(maintenanceTimeZoneCombo).bind(ProxyAssignmentWindow::getMaintenanceTimeZone,
                ProxyAssignmentWindow::setMaintenanceTimeZone);

        return maintenanceTimeZoneCombo;
    }

    /**
     * Create maintenance schedule translator
     *
     * @return maintenance schedule translator
     */
    public Label createMaintenanceScheduleTranslator() {
        final Label maintenanceScheduleTranslator = new LabelBuilder()
                .id(UIComponentIdProvider.MAINTENANCE_WINDOW_SCHEDULE_TRANSLATOR_ID)
                .name(i18n.getMessage(UIMessageIdProvider.CRON_VALIDATION_ERROR)).buildLabel();
        maintenanceScheduleTranslator.addStyleName(ValoTheme.LABEL_TINY);

        return maintenanceScheduleTranslator;
    }

    /**
     * Create maintenance help link
     *
     * @param uiProperties
     *          UiProperties
     *
     * @return Maintenance window help link
     */
    public Link createMaintenanceHelpLink(final UiProperties uiProperties) {
        final String maintenanceWindowHelpUrl = uiProperties.getLinks().getDocumentation().getMaintenanceWindowView();
        final Link maintenanceHelpLink = new Link("", new ExternalResource(maintenanceWindowHelpUrl));

        maintenanceHelpLink.setTargetName("_blank");
        maintenanceHelpLink.setIcon(VaadinIcons.QUESTION_CIRCLE);
        maintenanceHelpLink.setDescription(i18n.getMessage("tooltip.documentation.link"));

        return maintenanceHelpLink;
    }

    public Link createConfirmationHelpLink(final UiProperties uiProperties) {
        final String confirmationFlowHelpUrl = uiProperties.getLinks().getDocumentation().getUserConsentAndConfirmationGuide();
        final Link confirmationHelpLink = new Link("", new ExternalResource(confirmationFlowHelpUrl));

        confirmationHelpLink.setTargetName("_blank");
        confirmationHelpLink.setIcon(VaadinIcons.QUESTION_CIRCLE);
        confirmationHelpLink.setDescription(i18n.getMessage("tooltip.documentation.link"));

        return confirmationHelpLink;
    }
}
