/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.window.components;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.ui.common.builder.BoundComponent;
import org.eclipse.hawkbit.ui.common.builder.FormComponentBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.common.data.providers.TargetFilterQueryDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyAdvancedRolloutGroup;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetFilterQuery;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.data.Binder;
import com.vaadin.data.StatusChangeListener;
import com.vaadin.data.converter.StringToFloatConverter;
import com.vaadin.data.validator.FloatRangeValidator;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.server.UserError;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

import static org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions.DIST_CHECKBOX_STYLE;

/**
 * Advance group row component
 */
public class AdvancedGroupRow {

    private final VaadinMessageSource i18n;
    private final TargetFilterQueryDataProvider targetFilterQueryDataProvider;

    private final Binder<ProxyAdvancedRolloutGroup> binder;

    private final TextField groupName;
    private final ComboBox<ProxyTargetFilterQuery> targetFilterQueryCombo;
    private final TextField targetPercentage;
    private final TextField triggerThreshold;
    private final TextField errorThreshold;
    private final CheckBox requireConfirmationToggle;
    private final boolean isConfirmationFlowEnabled;

    /**
     * Constructor for AdvancedGroupRow
     *
     * @param i18n
     *          VaadinMessageSource
     * @param targetFilterQueryDataProvider
     *          TargetFilterQueryDataProvider
     */
    public AdvancedGroupRow(final VaadinMessageSource i18n,
            final TargetFilterQueryDataProvider targetFilterQueryDataProvider,
            final boolean isConfirmationFlowEnabled) {
        this.i18n = i18n;
        this.targetFilterQueryDataProvider = targetFilterQueryDataProvider;
        this.isConfirmationFlowEnabled = isConfirmationFlowEnabled;

        this.binder = new Binder<>();

        this.groupName = createGroupName();
        this.targetFilterQueryCombo = createTargetFilterQueryCombo();
        this.targetPercentage = createTargetPercentage();
        this.triggerThreshold = createTriggerThreshold();
        this.errorThreshold = createErrorThreshold();
        this.requireConfirmationToggle = createConfirmationToggle();
    }

    private TextField createGroupName() {
        final TextField nameField = new TextFieldBuilder(RolloutGroup.NAME_MAX_SIZE)
                .prompt(i18n.getMessage("textfield.name")).buildTextComponent();
        nameField.setSizeUndefined();
        nameField.setStyleName("rollout-group-name");
        nameField.addStyleName(ValoTheme.TEXTAREA_SMALL);
        nameField.setWidth(12, Unit.EM);

        binder.forField(nameField).asRequired(i18n.getMessage(UIMessageIdProvider.MESSAGE_ERROR_NAMEREQUIRED))
                .bind(ProxyAdvancedRolloutGroup::getGroupName, ProxyAdvancedRolloutGroup::setGroupName);

        return nameField;
    }

    private ComboBox<ProxyTargetFilterQuery> createTargetFilterQueryCombo() {
        final BoundComponent<ComboBox<ProxyTargetFilterQuery>> boundTfqCombo = FormComponentBuilder
                .createTargetFilterQueryCombo(binder, null, targetFilterQueryDataProvider, i18n, null);
        boundTfqCombo.setRequired(false);

        return boundTfqCombo.getComponent();
    }

    private TextField createTargetPercentage() {
        final TextField targetPercentageField = new TextFieldBuilder(32)
                .prompt(i18n.getMessage("textfield.target.percentage")).buildTextComponent();
        targetPercentageField.setWidth(5, Unit.EM);

        binder.forField(targetPercentageField).asRequired(i18n.getMessage("textfield.target.percentage.required"))
                .withConverter(new StringToFloatConverter(i18n.getMessage("textfield.target.percentage.format.error")))
                .withValidator((value, context) -> {
                    final FloatRangeValidator validator = new FloatRangeValidator(
                            i18n.getMessage("message.rollout.field.value.range", 0, 100), 0F, 100F);
                    validator.setMinValueIncluded(false);
                    return validator.apply(value, context);
                }).bind(ProxyAdvancedRolloutGroup::getTargetPercentage, ProxyAdvancedRolloutGroup::setTargetPercentage);

        return targetPercentageField;
    }

    private TextField createTriggerThreshold() {
        final TextField triggerThresholdField = new TextFieldBuilder(32)
                .prompt(i18n.getMessage("prompt.trigger.threshold")).buildTextComponent();
        triggerThresholdField.setWidth(5, Unit.EM);

        binder.forField(triggerThresholdField).asRequired(i18n.getMessage("prompt.trigger.threshold.required")).bind(
                ProxyAdvancedRolloutGroup::getTriggerThresholdPercentage,
                ProxyAdvancedRolloutGroup::setTriggerThresholdPercentage);

        return triggerThresholdField;
    }

    private TextField createErrorThreshold() {
        final TextField errorThresholdField = new TextFieldBuilder(32).prompt(i18n.getMessage("prompt.error.threshold"))
              .buildTextComponent();
        errorThresholdField.setWidth(5, Unit.EM);

        binder.forField(errorThresholdField).asRequired(i18n.getMessage("prompt.error.threshold.required")).bind(
              ProxyAdvancedRolloutGroup::getErrorThresholdPercentage,
              ProxyAdvancedRolloutGroup::setErrorThresholdPercentage);

        return errorThresholdField;
    }

    public CheckBox createConfirmationToggle() {
        final CheckBox checkBox = new CheckBox();
        checkBox.setWidth(5, Unit.EM);
        checkBox.setStyleName(DIST_CHECKBOX_STYLE);
        binder.forField(checkBox).bind(ProxyAdvancedRolloutGroup::isConfirmationRequired, ProxyAdvancedRolloutGroup::setConfirmationRequired);
        checkBox.setValue(true);
        return checkBox;
    }

    /**
     * Add row to group layout
     *
     * @param layout
     *          Grid layout
     * @param index
     *          Row index
     */
    public void addRowToLayout(final GridLayout layout, final int index) {
        layout.addComponent(groupName, 0, index);
        layout.addComponent(targetFilterQueryCombo, 1, index);
        layout.addComponent(targetPercentage, 2, index);
        layout.addComponent(triggerThreshold, 3, index);
        layout.addComponent(errorThreshold, 4, index);
        if (isConfirmationFlowEnabled) {
            layout.addComponent(requireConfirmationToggle, 5, index);
            layout.setComponentAlignment(requireConfirmationToggle, Alignment.MIDDLE_LEFT);
        }
    }

    /**
     * Update Group components
     *
     * @param index
     *          Group index
     */
    public void updateComponentIds(final int index) {
        groupName.setId(UIComponentIdProvider.ROLLOUT_GROUP_LIST_GRID_ID + "." + index);
        targetFilterQueryCombo.setId(UIComponentIdProvider.ROLLOUT_TARGET_FILTER_COMBO_ID + "." + index);
        targetPercentage.setId(UIComponentIdProvider.ROLLOUT_GROUP_TARGET_PERC_ID + "." + index);
        triggerThreshold.setId(UIComponentIdProvider.ROLLOUT_TRIGGER_THRESOLD_ID + "." + index);
        errorThreshold.setId(UIComponentIdProvider.ROLLOUT_ERROR_THRESOLD_ID + "." + index);
        requireConfirmationToggle.setId(UIComponentIdProvider.ROLLOUT_CONFIRMATION_REQUIRED + "." + index);
    }

    /**
     * Sets the bean in vaadin binder
     *
     * @param bean
     *          ProxyAdvancedRolloutGroup
     */
    public void setBean(final ProxyAdvancedRolloutGroup bean) {
        binder.setBean(bean);
    }

    /**
     * @return Bean of advance rollout group
     */
    public ProxyAdvancedRolloutGroup getBean() {
        return binder.getBean();
    }

    /**
     * Add status change listener in rollout group binder
     *
     * @param listener
     *          StatusChangeListener
     */
    public void addStatusChangeListener(final StatusChangeListener listener) {
        binder.addStatusChangeListener(listener);
    }

    /**
     * @return whether the data entered in this row is valid
     */
    public boolean isValid() {
        return binder.isValid();
    }

    /**
     * Sets the user error in target percentage component
     *
     * @param error
     *          Component error
     */
    public void setError(final String error) {
        targetPercentage.setComponentError(new UserError(error));
    }

    /**
     * Remove the error from the target percentage component
     */
    public void resetError() {
        targetPercentage.setComponentError(null);
    }
}
