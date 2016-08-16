/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rollout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.RepositoryModelConstants;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.DistributionSetIdName;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.common.builder.WindowBuilder;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.filtermanagement.TargetFilterBeanQuery;
import org.eclipse.hawkbit.ui.management.footer.ActionTypeOptionGroupLayout;
import org.eclipse.hawkbit.ui.management.footer.ActionTypeOptionGroupLayout.ActionTypeOption;
import org.eclipse.hawkbit.ui.rollout.event.RolloutEvent;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus;

import com.google.common.base.Strings;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Validator;
import com.vaadin.data.util.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.data.validator.NullValidator;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Rollout add or update popup layout.
 */
@SpringComponent
@ViewScope
public class AddUpdateRolloutWindowLayout extends GridLayout {

    private static final long serialVersionUID = 2999293468801479916L;

    private static final String MESSAGE_ROLLOUT_FIELD_VALUE_RANGE = "message.rollout.field.value.range";

    private static final String MESSAGE_ENTER_NUMBER = "message.enter.number";

    @Autowired
    private ActionTypeOptionGroupLayout actionTypeOptionGroupLayout;

    @Autowired
    private transient RolloutManagement rolloutManagement;

    @Autowired
    private transient DistributionSetManagement distributionSetManagement;

    @Autowired
    private transient TargetManagement targetManagement;

    @Autowired
    private UINotification uiNotification;

    @Autowired
    private transient UiProperties uiProperties;

    @Autowired
    private transient EntityFactory entityFactory;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    private TextField rolloutName;

    private ComboBox distributionSet;

    private ComboBox targetFilterQueryCombo;

    private TextField noOfGroups;

    private Label groupSizeLabel;

    private TextField triggerThreshold;

    private TextField errorThreshold;

    private TextArea description;

    private OptionGroup errorThresholdOptionGroup;

    private CommonDialogWindow window;

    private Boolean editRolloutEnabled;

    private Rollout rolloutForEdit;

    private Long totalTargetsCount;

    private Label totalTargetsLabel;

    private TextArea targetFilterQuery;

    private final NullValidator nullValidator = new NullValidator(null, false);

    /**
     * Create components and layout.
     */
    public void init() {
        setSizeUndefined();
        createRequiredComponents();
        buildLayout();
    }

    /**
     * Get the window.
     *
     * @param rolloutId
     *            the rollout id
     * @return the window
     */
    public CommonDialogWindow getWindow(final Long rolloutId) {
        window = getWindow();
        populateData(rolloutId);
        return window;
    }

    public CommonDialogWindow getWindow() {
        resetComponents();
        return new WindowBuilder(SPUIDefinitions.CREATE_UPDATE_WINDOW).caption(i18n.get("caption.configure.rollout"))
                .content(this).saveButtonClickListener(event -> onRolloutSave()).layout(this).i18n(i18n)
                .helpLink(uiProperties.getLinks().getDocumentation().getRolloutView()).buildCommonDialogWindow();
    }

    /**
     * Reset the field values.
     */
    public void resetComponents() {
        editRolloutEnabled = Boolean.FALSE;
        rolloutName.clear();
        targetFilterQuery.clear();
        resetFields();
        enableFields();
        populateDistributionSet();
        populateTargetFilterQuery();
        setDefaultSaveStartGroupOption();
        totalTargetsLabel.setVisible(false);
        groupSizeLabel.setVisible(false);
        removeComponent(1, 2);
        addComponent(targetFilterQueryCombo, 1, 2);
        actionTypeOptionGroupLayout.selectDefaultOption();
        totalTargetsCount = 0L;
        rolloutForEdit = null;
    }

    private void resetFields() {
        rolloutName.removeStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
        noOfGroups.clear();
        noOfGroups.removeStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
        triggerThreshold.clear();
        triggerThreshold.removeStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
        errorThreshold.clear();
        errorThreshold.removeStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
        description.clear();
        description.removeStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
    }

    private void buildLayout() {

        setSpacing(Boolean.TRUE);
        setSizeUndefined();
        setRows(9);
        setColumns(3);
        setStyleName("marginTop");

        addComponent(getMandatoryLabel("textfield.name"), 0, 0);
        addComponent(rolloutName, 1, 0);
        rolloutName.addValidator(nullValidator);

        addComponent(getMandatoryLabel("prompt.distribution.set"), 0, 1);
        addComponent(distributionSet, 1, 1);
        distributionSet.addValidator(nullValidator);

        addComponent(getMandatoryLabel("prompt.target.filter"), 0, 2);
        addComponent(targetFilterQueryCombo, 1, 2);
        targetFilterQueryCombo.addValidator(nullValidator);
        targetFilterQuery.removeValidator(nullValidator);

        addComponent(totalTargetsLabel, 2, 2);

        addComponent(getMandatoryLabel("prompt.number.of.groups"), 0, 3);
        addComponent(noOfGroups, 1, 3);
        noOfGroups.addValidator(nullValidator);

        addComponent(groupSizeLabel, 2, 3);

        addComponent(getMandatoryLabel("prompt.tigger.threshold"), 0, 4);
        addComponent(triggerThreshold, 1, 4);
        triggerThreshold.addValidator(nullValidator);

        addComponent(getPercentHintLabel(), 2, 4);

        addComponent(getMandatoryLabel("prompt.error.threshold"), 0, 5);
        addComponent(errorThreshold, 1, 5);
        errorThreshold.addValidator(nullValidator);
        addComponent(errorThresholdOptionGroup, 2, 5);

        addComponent(getLabel("textfield.description"), 0, 6);
        addComponent(description, 1, 6, 2, 6);
        addComponent(actionTypeOptionGroupLayout, 0, 7, 2, 7);

        rolloutName.focus();
    }

    private Label getMandatoryLabel(final String key) {
        final Label mandatoryLabel = getLabel(i18n.get(key));
        mandatoryLabel.setContentMode(ContentMode.HTML);
        mandatoryLabel.setValue(mandatoryLabel.getValue().concat(" <span style='color:#ed473b'>*</span>"));
        return mandatoryLabel;
    }

    private Label getLabel(final String key) {
        return new LabelBuilder().name(i18n.get(key)).buildLabel();
    }

    private TextField createTextField(final String in18Key, final String id) {
        return new TextFieldBuilder().prompt(i18n.get(in18Key)).immediate(true).id(id).buildTextField();
    }

    private TextField createIntegerTextField(final String in18Key, final String id) {
        final TextField textField = createTextField(in18Key, id);
        textField.setNullRepresentation("");
        textField.setConverter(new StringToIntegerConverter());
        textField.setConversionError(i18n.get(MESSAGE_ENTER_NUMBER));
        textField.setSizeUndefined();
        return textField;
    }

    private static Label getPercentHintLabel() {
        final Label percentSymbol = new Label("%");
        percentSymbol.addStyleName(ValoTheme.LABEL_TINY + " " + ValoTheme.LABEL_BOLD);
        percentSymbol.setSizeUndefined();
        return percentSymbol;
    }

    private void createRequiredComponents() {
        rolloutName = createRolloutNameField();
        distributionSet = createDistributionSetCombo();
        populateDistributionSet();

        targetFilterQueryCombo = createTargetFilterQueryCombo();
        populateTargetFilterQuery();

        noOfGroups = createNoOfGroupsField();
        groupSizeLabel = createCountLabel();
        triggerThreshold = createTriggerThreshold();
        errorThreshold = createErrorThreshold();
        description = createDescription();
        errorThresholdOptionGroup = createErrorThresholdOptionGroup();
        setDefaultSaveStartGroupOption();
        actionTypeOptionGroupLayout.selectDefaultOption();
        totalTargetsLabel = createCountLabel();
        targetFilterQuery = createTargetFilterQuery();
        actionTypeOptionGroupLayout.addStyleName(SPUIStyleDefinitions.ROLLOUT_ACTION_TYPE_LAYOUT);
    }

    private static Label createCountLabel() {
        final Label groupSize = new LabelBuilder().visible(false).name("").buildLabel();
        groupSize.addStyleName(ValoTheme.LABEL_TINY + " " + "rollout-target-count-message");
        groupSize.setImmediate(true);
        groupSize.setSizeUndefined();
        return groupSize;
    }

    private static TextArea createTargetFilterQuery() {
        final TextArea filterField = SPUIComponentProvider.getTextArea(null, "text-area-style",
                ValoTheme.TEXTFIELD_TINY, false, null, null,
                SPUILabelDefinitions.TARGET_FILTER_QUERY_TEXT_FIELD_LENGTH);
        filterField.setId(SPUIComponentIdProvider.ROLLOUT_TARGET_FILTER_QUERY_FIELD);
        filterField.setNullRepresentation(HawkbitCommonUtil.SP_STRING_EMPTY);
        filterField.setEnabled(false);
        filterField.setSizeUndefined();
        return filterField;
    }

    private OptionGroup createErrorThresholdOptionGroup() {
        final OptionGroup errorThresoldOptions = new OptionGroup();
        for (final ERRORTHRESOLDOPTIONS option : ERRORTHRESOLDOPTIONS.values()) {
            errorThresoldOptions.addItem(option.getValue());
        }
        errorThresoldOptions.setId(SPUIComponentIdProvider.ROLLOUT_ERROR_THRESOLD_OPTION_ID);
        errorThresoldOptions.addStyleName(ValoTheme.OPTIONGROUP_HORIZONTAL);
        errorThresoldOptions.addStyleName(SPUIStyleDefinitions.ROLLOUT_OPTION_GROUP);
        errorThresoldOptions.setSizeUndefined();
        errorThresoldOptions.addValueChangeListener(this::listenerOnErrorThresoldOptionChange);
        return errorThresoldOptions;
    }

    private void listenerOnErrorThresoldOptionChange(final ValueChangeEvent event) {
        errorThreshold.clear();
        errorThreshold.removeAllValidators();
        if (event.getProperty().getValue().equals(ERRORTHRESOLDOPTIONS.COUNT.getValue())) {
            errorThreshold.addValidator(new ErrorThresoldOptionValidator());
        } else {
            errorThreshold.addValidator(new ThresholdFieldValidator());
        }
        errorThreshold.getValidators();
    }

    private ComboBox createTargetFilterQueryCombo() {
        final ComboBox targetFilter = SPUIComponentProvider.getComboBox(null, "", "", null, ValoTheme.COMBOBOX_SMALL,
                false, "", i18n.get("prompt.target.filter"));
        targetFilter.setImmediate(true);
        targetFilter.setPageLength(7);
        targetFilter.setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME);
        targetFilter.setId(SPUIComponentIdProvider.ROLLOUT_TARGET_FILTER_COMBO_ID);
        targetFilter.setSizeUndefined();
        targetFilter.addValueChangeListener(this::onTargetFilterChange);
        return targetFilter;
    }

    private void onTargetFilterChange(final ValueChangeEvent event) {
        final String filterQueryString = getTargetFilterQuery();
        if (!Strings.isNullOrEmpty(filterQueryString)) {
            totalTargetsCount = targetManagement.countTargetByTargetFilterQuery(filterQueryString);
            totalTargetsLabel.setValue(getTotalTargetMessage());
            totalTargetsLabel.setVisible(true);
        } else {
            totalTargetsCount = 0L;
            totalTargetsLabel.setVisible(false);
        }
        onGroupNumberChange(event);
    }

    private String getTotalTargetMessage() {
        return new StringBuilder(i18n.get("label.target.filter.count")).append(totalTargetsCount).toString();
    }

    private String getTargetPerGroupMessage(final String value) {
        return new StringBuilder(i18n.get("label.target.per.group")).append(value).toString();
    }

    private void populateTargetFilterQuery() {
        final Container container = createTargetFilterComboContainer();
        targetFilterQueryCombo.setContainerDataSource(container);
    }

    private Container createTargetFilterComboContainer() {
        final BeanQueryFactory<TargetFilterBeanQuery> targetFilterQF = new BeanQueryFactory<>(
                TargetFilterBeanQuery.class);
        return new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_NAME),
                targetFilterQF);
    }

    private void onRolloutSave() {
        if (editRolloutEnabled) {
            editRollout();
        } else {
            createRollout();
        }
    }

    private void editRollout() {
        if (duplicateCheckForEdit() && rolloutForEdit != null) {
            rolloutForEdit.setName(rolloutName.getValue());
            rolloutForEdit.setDescription(description.getValue());
            final DistributionSetIdName distributionSetIdName = (DistributionSetIdName) distributionSet.getValue();
            rolloutForEdit.setDistributionSet(
                    distributionSetManagement.findDistributionSetById(distributionSetIdName.getId()));
            rolloutForEdit.setActionType(getActionType());
            rolloutForEdit.setForcedTime(getForcedTimeStamp());
            final int amountGroup = Integer.parseInt(noOfGroups.getValue());
            final int errorThresoldPercent = getErrorThresoldPercentage(amountGroup);

            for (final RolloutGroup rolloutGroup : rolloutForEdit.getRolloutGroups()) {
                rolloutGroup.setErrorConditionExp(triggerThreshold.getValue());
                rolloutGroup.setSuccessConditionExp(String.valueOf(errorThresoldPercent));
            }
            final Rollout updatedRollout = rolloutManagement.updateRollout(rolloutForEdit);
            uiNotification
                    .displaySuccess(i18n.get("message.update.success", new Object[] { updatedRollout.getName() }));
            eventBus.publish(this, RolloutEvent.UPDATE_ROLLOUT);
        }
    }

    private boolean duplicateCheckForEdit() {
        final String rolloutNameVal = getRolloutName();
        if (!rolloutForEdit.getName().equals(rolloutNameVal)
                && rolloutManagement.findRolloutByName(rolloutNameVal) != null) {
            uiNotification.displayValidationError(i18n.get("message.rollout.duplicate.check", rolloutNameVal));
            return false;
        }
        return true;
    }

    private long getForcedTimeStamp() {
        return (((ActionTypeOptionGroupLayout.ActionTypeOption) actionTypeOptionGroupLayout.getActionTypeOptionGroup()
                .getValue()) == ActionTypeOption.AUTO_FORCED)
                        ? actionTypeOptionGroupLayout.getForcedTimeDateField().getValue().getTime()
                        : RepositoryModelConstants.NO_FORCE_TIME;
    }

    private ActionType getActionType() {
        return ((ActionTypeOptionGroupLayout.ActionTypeOption) actionTypeOptionGroupLayout.getActionTypeOptionGroup()
                .getValue()).getActionType();
    }

    private void createRollout() {
        if (duplicateCheck()) {
            final Rollout rolloutToCreate = saveRollout();
            uiNotification.displaySuccess(i18n.get("message.save.success", new Object[] { rolloutToCreate.getName() }));
            eventBus.publish(this, RolloutEvent.CREATE_ROLLOUT);
        }
    }

    private Rollout saveRollout() {
        Rollout rolloutToCreate = entityFactory.generateRollout();
        final int amountGroup = Integer.parseInt(noOfGroups.getValue());
        final String targetFilter = getTargetFilterQuery();
        final int errorThresoldPercent = getErrorThresoldPercentage(amountGroup);

        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder()
                .successAction(RolloutGroupSuccessAction.NEXTGROUP, null)
                .successCondition(RolloutGroupSuccessCondition.THRESHOLD, triggerThreshold.getValue())
                .errorCondition(RolloutGroupErrorCondition.THRESHOLD, String.valueOf(errorThresoldPercent))
                .errorAction(RolloutGroupErrorAction.PAUSE, null).build();

        final DistributionSetIdName distributionSetIdName = (DistributionSetIdName) distributionSet.getValue();
        rolloutToCreate.setName(rolloutName.getValue());
        rolloutToCreate.setDescription(description.getValue());
        rolloutToCreate.setTargetFilterQuery(targetFilter);
        rolloutToCreate
                .setDistributionSet(distributionSetManagement.findDistributionSetById(distributionSetIdName.getId()));
        rolloutToCreate.setActionType(getActionType());
        rolloutToCreate.setForcedTime(getForcedTimeStamp());

        rolloutToCreate = rolloutManagement.createRolloutAsync(rolloutToCreate, amountGroup, conditions);
        return rolloutToCreate;
    }

    private String getTargetFilterQuery() {
        if (null != targetFilterQueryCombo.getValue()
                && HawkbitCommonUtil.trimAndNullIfEmpty((String) targetFilterQueryCombo.getValue()) != null) {
            final Item filterItem = targetFilterQueryCombo.getContainerDataSource()
                    .getItem(targetFilterQueryCombo.getValue());
            return (String) filterItem.getItemProperty("query").getValue();
        }
        return null;
    }

    private int getErrorThresoldPercentage(final int amountGroup) {
        int errorThresoldPercent = Integer.parseInt(errorThreshold.getValue());
        if (errorThresholdOptionGroup.getValue().equals(ERRORTHRESOLDOPTIONS.COUNT.getValue())) {
            final int groupSize = (int) Math.ceil((double) totalTargetsCount / (double) amountGroup);
            final int erroThresoldCount = Integer.parseInt(errorThreshold.getValue());
            errorThresoldPercent = (int) Math.ceil(((float) erroThresoldCount / (float) groupSize) * 100);
        }
        return errorThresoldPercent;
    }

    private boolean duplicateCheck() {
        if (rolloutManagement.findRolloutByName(getRolloutName()) != null) {
            uiNotification.displayValidationError(
                    i18n.get("message.rollout.duplicate.check", new Object[] { getRolloutName() }));
            return false;
        }
        return true;
    }

    private void setDefaultSaveStartGroupOption() {
        errorThresholdOptionGroup.setValue(ERRORTHRESOLDOPTIONS.PERCENT.getValue());
    }

    private TextArea createDescription() {
        final TextArea descriptionField = SPUIComponentProvider.getTextArea(null, "text-area-style",
                ValoTheme.TEXTAREA_TINY, false, null, i18n.get("textfield.description"),
                SPUILabelDefinitions.TEXT_AREA_MAX_LENGTH);
        descriptionField.setId(SPUIComponentIdProvider.ROLLOUT_DESCRIPTION_ID);
        descriptionField.setNullRepresentation(HawkbitCommonUtil.SP_STRING_EMPTY);
        descriptionField.setSizeUndefined();
        return descriptionField;
    }

    private TextField createErrorThreshold() {
        final TextField errorField = createIntegerTextField("prompt.error.threshold",
                SPUIComponentIdProvider.ROLLOUT_ERROR_THRESOLD_ID);
        errorField.addValidator(new ThresholdFieldValidator());
        errorField.setMaxLength(7);
        return errorField;
    }

    private TextField createTriggerThreshold() {
        final TextField thresholdField = createIntegerTextField("prompt.tigger.threshold",
                SPUIComponentIdProvider.ROLLOUT_TRIGGER_THRESOLD_ID);
        thresholdField.addValidator(new ThresholdFieldValidator());
        return thresholdField;
    }

    private TextField createNoOfGroupsField() {
        final TextField noOfGroupsField = createIntegerTextField("prompt.number.of.groups",
                SPUIComponentIdProvider.ROLLOUT_NO_OF_GROUPS_ID);
        noOfGroupsField.addValidator(new GroupNumberValidator());
        noOfGroupsField.setMaxLength(3);
        noOfGroupsField.addValueChangeListener(this::onGroupNumberChange);
        return noOfGroupsField;
    }

    private void onGroupNumberChange(final ValueChangeEvent event) {
        if (event.getProperty().getValue() != null && noOfGroups.isValid()) {
            groupSizeLabel.setValue(getTargetPerGroupMessage(String.valueOf(getGroupSize())));
            groupSizeLabel.setVisible(true);
        } else {
            groupSizeLabel.setVisible(false);
        }
    }

    private ComboBox createDistributionSetCombo() {
        final ComboBox dsSet = SPUIComponentProvider.getComboBox(null, "", "", null, ValoTheme.COMBOBOX_SMALL, false,
                "", i18n.get("prompt.distribution.set"));
        dsSet.setImmediate(true);
        dsSet.setPageLength(7);
        dsSet.setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME);
        dsSet.setId(SPUIComponentIdProvider.ROLLOUT_DS_ID);
        dsSet.setSizeUndefined();
        return dsSet;
    }

    private void populateDistributionSet() {
        final Container container = createDsComboContainer();
        distributionSet.setContainerDataSource(container);
    }

    private Container createDsComboContainer() {
        final BeanQueryFactory<DistributionBeanQuery> distributionQF = new BeanQueryFactory<>(
                DistributionBeanQuery.class);
        return new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_DIST_ID_NAME),
                distributionQF);
    }

    private TextField createRolloutNameField() {
        final TextField rolloutNameField = createTextField("textfield.name",
                SPUIComponentIdProvider.ROLLOUT_NAME_FIELD_ID);
        rolloutNameField.setSizeUndefined();
        return rolloutNameField;
    }

    private String getRolloutName() {
        return HawkbitCommonUtil.trimAndNullIfEmpty(rolloutName.getValue());
    }

    class ErrorThresoldOptionValidator implements Validator {
        private static final long serialVersionUID = 9049939751976326550L;

        @Override
        public void validate(final Object value) {
            try {
                if (isNoOfGroupsOrTargetFilterEmpty()) {
                    uiNotification
                            .displayValidationError(i18n.get("message.rollout.noofgroups.or.targetfilter.missing"));
                } else {
                    if (value != null) {
                        final int groupSize = getGroupSize();
                        new IntegerRangeValidator(i18n.get(MESSAGE_ROLLOUT_FIELD_VALUE_RANGE, 0, groupSize), 0,
                                groupSize).validate(Integer.valueOf(value.toString()));
                    }
                }
            }
            // suppress the need of preserve original exception, will blow
            // up the
            // log and not necessary here
            catch (final InvalidValueException ex) {
                // we have to throw the exception here, otherwise the UI won't
                // show the vaadin validation error!
                throw ex;
            }
        }

        private boolean isNoOfGroupsOrTargetFilterEmpty() {
            return HawkbitCommonUtil.trimAndNullIfEmpty(noOfGroups.getValue()) == null
                    || (HawkbitCommonUtil.trimAndNullIfEmpty((String) targetFilterQueryCombo.getValue()) == null
                            && targetFilterQuery.getValue() == null);
        }
    }

    private int getGroupSize() {
        return (int) Math.ceil((double) totalTargetsCount / Double.parseDouble(noOfGroups.getValue()));
    }

    class ThresholdFieldValidator implements Validator {
        private static final long serialVersionUID = 9049939751976326550L;

        @Override
        public void validate(final Object value) {
            try {
                if (value != null) {
                    new IntegerRangeValidator(i18n.get(MESSAGE_ROLLOUT_FIELD_VALUE_RANGE, 0, 100), 0, 100)
                            .validate(Integer.valueOf(value.toString()));
                }
            }
            // suppress the need of preserve original exception, will blow
            // up the
            // log and not necessary here
            catch (final InvalidValueException ex) {
                // we have to throw the exception here, otherwise the UI won't
                // show the vaadin validation error!
                throw ex;
            }
        }
    }

    class GroupNumberValidator implements Validator {
        private static final long serialVersionUID = 9049939751976326550L;

        @Override
        public void validate(final Object value) {
            try {
                if (value != null) {
                    new IntegerRangeValidator(i18n.get(MESSAGE_ROLLOUT_FIELD_VALUE_RANGE, 0, 500), 0, 500)
                            .validate(Integer.valueOf(value.toString()));
                }
            }
            // suppress the need of preserve original exception, will blow
            // up the
            // log and not necessary here
            catch (final InvalidValueException ex) {
                // we have to throw the exception here, otherwise the UI won't
                // show the vaadin validation error!
                throw ex;
            }
        }
    }

    /**
     *
     * Populate rollout details.
     *
     * @param rolloutId
     *            rollout id
     */
    private void populateData(final Long rolloutId) {
        if (rolloutId == null) {
            return;
        }

        editRolloutEnabled = Boolean.TRUE;
        rolloutForEdit = rolloutManagement.findRolloutById(rolloutId);
        rolloutName.setValue(rolloutForEdit.getName());
        description.setValue(rolloutForEdit.getDescription());
        distributionSet.setValue(DistributionSetIdName.generate(rolloutForEdit.getDistributionSet()));
        final List<RolloutGroup> rolloutGroups = rolloutForEdit.getRolloutGroups();
        setThresholdValues(rolloutGroups);
        setActionType(rolloutForEdit);
        if (rolloutForEdit.getStatus() != RolloutStatus.READY) {
            disableRequiredFieldsOnEdit();
        }

        noOfGroups.setEnabled(false);
        targetFilterQuery.setValue(rolloutForEdit.getTargetFilterQuery());
        removeComponent(1, 2);
        targetFilterQueryCombo.removeValidator(nullValidator);
        addComponent(targetFilterQuery, 1, 2);
        targetFilterQuery.addValidator(nullValidator);

        totalTargetsCount = targetManagement.countTargetByTargetFilterQuery(rolloutForEdit.getTargetFilterQuery());
        totalTargetsLabel.setValue(getTotalTargetMessage());
        totalTargetsLabel.setVisible(true);

        window.setOrginaleValues();
    }

    private void disableRequiredFieldsOnEdit() {
        distributionSet.setEnabled(false);
        errorThreshold.setEnabled(false);
        triggerThreshold.setEnabled(false);
        actionTypeOptionGroupLayout.getActionTypeOptionGroup().setEnabled(false);
        errorThresholdOptionGroup.setEnabled(false);
        actionTypeOptionGroupLayout.addStyleName(SPUIStyleDefinitions.DISABLE_ACTION_TYPE_LAYOUT);
    }

    private void enableFields() {
        distributionSet.setEnabled(true);
        errorThreshold.setEnabled(true);
        triggerThreshold.setEnabled(true);
        actionTypeOptionGroupLayout.getActionTypeOptionGroup().setEnabled(true);
        actionTypeOptionGroupLayout.removeStyleName(SPUIStyleDefinitions.DISABLE_ACTION_TYPE_LAYOUT);
        noOfGroups.setEnabled(true);
        targetFilterQueryCombo.setEnabled(true);
        errorThresholdOptionGroup.setEnabled(true);
    }

    private void setActionType(final Rollout rollout) {
        for (final ActionTypeOptionGroupLayout.ActionTypeOption groupAction : ActionTypeOptionGroupLayout.ActionTypeOption
                .values()) {
            if (groupAction.getActionType() == rollout.getActionType()) {
                actionTypeOptionGroupLayout.getActionTypeOptionGroup().setValue(groupAction);
                final SimpleDateFormat format = new SimpleDateFormat(SPUIDefinitions.LAST_QUERY_DATE_FORMAT);
                format.setTimeZone(SPDateTimeUtil.getBrowserTimeZone());
                actionTypeOptionGroupLayout.getForcedTimeDateField().setValue(new Date(rollout.getForcedTime()));
                break;
            }
        }
    }

    /**
     * @param rolloutGroups
     */
    private void setThresholdValues(final List<RolloutGroup> rolloutGroups) {
        if (rolloutGroups != null && !rolloutGroups.isEmpty()) {
            errorThreshold.setValue(rolloutGroups.get(0).getErrorConditionExp());
            triggerThreshold.setValue(rolloutGroups.get(0).getSuccessConditionExp());
            noOfGroups.setValue(String.valueOf(rolloutGroups.size()));
        } else {
            errorThreshold.setValue("0");
            triggerThreshold.setValue("0");
            noOfGroups.setValue("0");
        }
    }

    enum SAVESTARTOPTIONS {
        SAVE("Save"), START("Start");

        String value;

        private SAVESTARTOPTIONS(final String val) {
            this.value = val;
        }

        /**
         * @return the value
         */
        public String getValue() {
            return value;
        }
    }

    enum ERRORTHRESOLDOPTIONS {
        PERCENT("%"), COUNT("Count");

        String value;

        private ERRORTHRESOLDOPTIONS(final String val) {
            this.value = val;
        }

        /**
         * @return the value
         */
        public String getValue() {
            return value;
        }
    }

}
