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
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSetIdName;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.CommonDialog;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.filtermanagement.TargetFilterBeanQuery;
import org.eclipse.hawkbit.ui.management.footer.ActionTypeOptionGroupLayout;
import org.eclipse.hawkbit.ui.management.footer.ActionTypeOptionGroupLayout.ActionTypeOption;
import org.eclipse.hawkbit.ui.rollout.event.RolloutEvent;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
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
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * 
 * Rollout add or update popup layout.
 *
 */
@SpringComponent
@ViewScope
public class AddUpdateRolloutWindowLayout extends CommonDialog {

    private static final long serialVersionUID = 2999293468801479916L;

    private static final String MESSAGE_ROLLOUT_FIELD_VALUE_RANGE = "message.rollout.field.value.range";

    private static final String MESSAGE_ENTER_NUMBER = "message.enter.number";

    private static final String NUMBER_REGEXP = "[-]?[0-9]*\\.?,?[0-9]+";

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
    private I18N i18n;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    private Label madatoryLabel;

    private TextField rolloutName;

    private ComboBox distributionSet;

    private ComboBox targetFilterQueryCombo;

    private TextField noOfGroups;

    private Label groupSizeLabel;

    private TextField triggerThreshold;

    private TextField errorThreshold;

    private TextArea description;

    private Button saveRolloutBtn;

    private Button discardRollloutBtn;

    private OptionGroup errorThresholdOptionGroup;

    private Link linkToHelp;

    private Window addUpdateRolloutWindow;

    private Boolean editRolloutEnabled;

    private Rollout rolloutForEdit;

    private Long totalTargetsCount;

    private Label totalTargetsLabel;

    private TextArea targetFilterQuery;

    public AddUpdateRolloutWindowLayout() {
        super();
    }

    public AddUpdateRolloutWindowLayout(final String title, final FormLayout form, final String helpLink) {
        super(title, form, helpLink);
    }

    /**
     * Create components and layout.
     */
    public void init() {
        // createRequiredComponents();
        // buildLayout();
        setCaption(i18n.get("caption.configure.rollout"));
        initForm();
        resetFields();
    }

    public Window getWindow() {
        addUpdateRolloutWindow = SPUIComponentProvider.getWindow(i18n.get("caption.configure.rollout"), null,
                SPUIDefinitions.CREATE_UPDATE_WINDOW);
        addUpdateRolloutWindow.setContent(this);
        return addUpdateRolloutWindow;
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
        targetFilterQuery.setVisible(false);
        targetFilterQueryCombo.setVisible(true);
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
        final VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setSpacing(Boolean.TRUE);
        mainLayout.setSizeUndefined();

        mainLayout.addComponents(madatoryLabel, rolloutName, distributionSet, getTargetFilterLayout(),
                getGroupDetailsLayout(), getTriggerThresoldLayout(), getErrorThresholdLayout(), description,
                actionTypeOptionGroupLayout, linkToHelp, getSaveDiscardButtonLayout());

        mainLayout.addComponent(new Label("TEST TEST TEST"));
        mainLayout.setComponentAlignment(linkToHelp, Alignment.BOTTOM_RIGHT);
        rolloutName.focus();
    }

    private HorizontalLayout getGroupDetailsLayout() {
        createGroupSizeLabel();
        createNoOfGroupsField();
        final HorizontalLayout groupLayout = new HorizontalLayout();
        groupLayout.setCaption(i18n.get("prompt.number.of.groups"));
        groupLayout.setSizeFull();
        groupLayout.addComponents(noOfGroups, groupSizeLabel);
        groupLayout.setExpandRatio(noOfGroups, 3.0F);
        groupLayout.setExpandRatio(groupSizeLabel, 1.0F);
        groupLayout.setComponentAlignment(groupSizeLabel, Alignment.MIDDLE_LEFT);
        return groupLayout;
    }

    private HorizontalLayout getErrorThresholdLayout() {
        createErrorThreshold();
        createErrorThresholdOptionGroup();
        final HorizontalLayout errorThresoldLayout = new HorizontalLayout();
        errorThresoldLayout.setSizeFull();
        errorThresoldLayout.addComponents(errorThreshold, errorThresholdOptionGroup);
        errorThresoldLayout.setExpandRatio(errorThreshold, 3.0F);
        errorThresoldLayout.setExpandRatio(errorThresholdOptionGroup, 1.0F);
        errorThresoldLayout.setCaption(i18n.get("prompt.error.threshold"));
        return errorThresoldLayout;
    }

    private HorizontalLayout getTargetFilterLayout() {
        final HorizontalLayout targetFilterLayout = new HorizontalLayout();
        targetFilterLayout.setSizeFull();
        targetFilterLayout.addComponents(targetFilterQueryCombo, targetFilterQuery, totalTargetsLabel);
        targetFilterLayout.setExpandRatio(targetFilterQueryCombo, 0.71F);
        targetFilterLayout.setExpandRatio(targetFilterQuery, 0.70F);
        targetFilterLayout.setExpandRatio(totalTargetsLabel, 0.29F);
        targetFilterLayout.setComponentAlignment(totalTargetsLabel, Alignment.MIDDLE_LEFT);
        return targetFilterLayout;
    }

    private HorizontalLayout getTriggerThresoldLayout() {
        createTriggerThresold();
        final HorizontalLayout triggerThresholdLayout = new HorizontalLayout();
        triggerThresholdLayout.setCaption(i18n.get("prompt.tigger.thresold"));
        triggerThresholdLayout.setSizeFull();
        final Label percentSymbol = getPercentHintLabel();
        triggerThresholdLayout.addComponents(triggerThreshold, percentSymbol);
        triggerThresholdLayout.setExpandRatio(triggerThreshold, 1.0F);
        triggerThresholdLayout.setExpandRatio(percentSymbol, 5.0F);

        return triggerThresholdLayout;
    }

    private Label getPercentHintLabel() {
        final Label percentSymbol = new Label("%");
        percentSymbol.addStyleName(ValoTheme.LABEL_TINY + " " + ValoTheme.LABEL_BOLD);
        percentSymbol.setSizeFull();
        return percentSymbol;
    }

    private HorizontalLayout getSaveDiscardButtonLayout() {
        final HorizontalLayout buttonsLayout = new HorizontalLayout();
        buttonsLayout.setSizeFull();
        buttonsLayout.addComponents(saveRolloutBtn, discardRollloutBtn);
        buttonsLayout.setComponentAlignment(saveRolloutBtn, Alignment.BOTTOM_LEFT);
        buttonsLayout.setComponentAlignment(discardRollloutBtn, Alignment.BOTTOM_RIGHT);
        buttonsLayout.addStyleName("window-style");
        return buttonsLayout;
    }

    private void initForm() {

        getFormLayout().addComponent(createRolloutNameField());
        getFormLayout().addComponent(createDistributionSetCombo());
        populateDistributionSet();
        getFormLayout().addComponent(createTargetFilterQueryCombo());
        populateTargetFilterQuery();
        getFormLayout().addComponent(getGroupDetailsLayout());
        getFormLayout().addComponent(getTriggerThresoldLayout());
        getFormLayout().addComponent(getErrorThresholdLayout());
        getFormLayout().addComponent(createDescription());
        getFormLayout().addComponent(actionTypeOptionGroupLayout);
        actionTypeOptionGroupLayout.selectDefaultOption();

        // getFormLayout().addComponent(createTotalTargetsLabel());
        // getFormLayout().addComponent(createTargetFilterQuery());
    }

    private void createRequiredComponents() {
        madatoryLabel = createMandatoryLabel();
        rolloutName = createRolloutNameField();
        distributionSet = createDistributionSetCombo();
        populateDistributionSet();

        targetFilterQueryCombo = createTargetFilterQueryCombo();
        populateTargetFilterQuery();

        // noOfGroups = createNoOfGroupsField();
        // groupSizeLabel = createGroupSizeLabel();
        // triggerThreshold = createTriggerThresold();
        // errorThreshold = createErrorThresold();
        // description = createDescription();
        // errorThresholdOptionGroup = createErrorThresholdOptionGroup();
        setDefaultSaveStartGroupOption();
        saveRolloutBtn = createSaveButton();
        discardRollloutBtn = createDiscardButton();
        actionTypeOptionGroupLayout.selectDefaultOption();

        totalTargetsLabel = createTotalTargetsLabel();
        targetFilterQuery = createTargetFilterQuery();

        linkToHelp = SPUIComponentProvider.getHelpLink(uiProperties.getLinks().getDocumentation().getRolloutView());
        actionTypeOptionGroupLayout.addStyleName(SPUIStyleDefinitions.ROLLOUT_ACTION_TYPE_LAYOUT);

    }

    private void createGroupSizeLabel() {
        groupSizeLabel = SPUIComponentProvider.getLabel("", SPUILabelDefinitions.SP_LABEL_SIMPLE);
        groupSizeLabel.addStyleName(ValoTheme.LABEL_TINY + " " + "rollout-target-count-message");
        groupSizeLabel.setImmediate(true);
        groupSizeLabel.setVisible(false);
        groupSizeLabel.setSizeUndefined();
    }

    private TextArea createTargetFilterQuery() {
        final TextArea filterField = SPUIComponentProvider.getTextArea(null, "text-area-style",
                ValoTheme.TEXTFIELD_TINY, false, null, null,
                SPUILabelDefinitions.TARGET_FILTER_QUERY_TEXT_FIELD_LENGTH);
        filterField.setId(SPUIComponetIdProvider.ROLLOUT_TARGET_FILTER_QUERY_FIELD);
        filterField.setNullRepresentation(HawkbitCommonUtil.SP_STRING_EMPTY);
        filterField.setVisible(false);
        filterField.setEnabled(false);
        filterField.setSizeFull();
        return filterField;
    }

    private Label createTotalTargetsLabel() {
        final Label targetCountLabel = SPUIComponentProvider.getLabel("", SPUILabelDefinitions.SP_LABEL_SIMPLE);
        targetCountLabel.addStyleName(ValoTheme.LABEL_TINY + " " + "rollout-target-count-message");
        targetCountLabel.setImmediate(true);
        targetCountLabel.setVisible(false);
        targetCountLabel.setSizeUndefined();
        return targetCountLabel;
    }

    private void createErrorThresholdOptionGroup() {
        errorThresholdOptionGroup = new OptionGroup();
        for (final ERRORTHRESOLDOPTIONS option : ERRORTHRESOLDOPTIONS.values()) {
            errorThresholdOptionGroup.addItem(option.getValue());
        }
        errorThresholdOptionGroup.setId(SPUIComponetIdProvider.ROLLOUT_ERROR_THRESOLD_OPTION_ID);
        errorThresholdOptionGroup.addStyleName(ValoTheme.OPTIONGROUP_HORIZONTAL);
        errorThresholdOptionGroup.addStyleName(SPUIStyleDefinitions.ROLLOUT_OPTION_GROUP);
        errorThresholdOptionGroup.setSizeUndefined();
        errorThresholdOptionGroup.addValueChangeListener(event -> onErrorThresoldOptionChange(event));
        setDefaultSaveStartGroupOption();
    }

    private void onErrorThresoldOptionChange(final ValueChangeEvent event) {
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
        targetFilterQueryCombo = SPUIComponentProvider.getComboBox(i18n.get("prompt.target.filter"), "", "", null, null,
                true, "", i18n.get("prompt.target.filter"));
        targetFilterQueryCombo.setImmediate(true);
        targetFilterQueryCombo.setPageLength(7);
        targetFilterQueryCombo.setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME);
        targetFilterQueryCombo.setId(SPUIComponetIdProvider.ROLLOUT_TARGET_FILTER_COMBO_ID);
        targetFilterQueryCombo.setSizeFull();
        targetFilterQueryCombo.addValueChangeListener(event -> onTargetFilterChange());
        return targetFilterQueryCombo;
    }

    private void onTargetFilterChange() {
        final String filterQueryString = getTargetFilterQuery();
        if (!Strings.isNullOrEmpty(filterQueryString)) {
            totalTargetsCount = targetManagement.countTargetByTargetFilterQuery(filterQueryString);
            totalTargetsLabel.setValue(getTotalTargetMessage());
            totalTargetsLabel.setVisible(true);
        } else {
            totalTargetsCount = 0L;
            totalTargetsLabel.setVisible(false);
        }
        onGroupNumberChange();
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

    private Button createDiscardButton() {
        final Button discardRollloutBtn = SPUIComponentProvider.getButton(
                SPUIComponetIdProvider.ROLLOUT_CREATE_UPDATE_DISCARD_ID, "", "", "", true, FontAwesome.TIMES,
                SPUIButtonStyleSmallNoBorder.class);
        discardRollloutBtn.addClickListener(event -> onDiscard());
        return discardRollloutBtn;
    }

    private Button createSaveButton() {
        final Button saveRolloutBtn = SPUIComponentProvider.getButton(
                SPUIComponetIdProvider.ROLLOUT_CREATE_UPDATE_SAVE_ID, "", "", "", true, FontAwesome.SAVE,
                SPUIButtonStyleSmallNoBorder.class);
        saveRolloutBtn.addClickListener(event -> onRolloutSave());
        saveRolloutBtn.setImmediate(true);
        return saveRolloutBtn;
    }

    private void onDiscard() {
        closeThisWindow();
    }

    private void onRolloutSave() {
        if (editRolloutEnabled) {
            editRollout();
        } else {
            createRollout();
        }
    }

    private void editRollout() {
        if (mandatoryCheckForEdit() && validateFields() && duplicateCheckForEdit() && null != rolloutForEdit) {
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
            closeThisWindow();
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
                        : Action.NO_FORCE_TIME;
    }

    private ActionType getActionType() {
        return ((ActionTypeOptionGroupLayout.ActionTypeOption) actionTypeOptionGroupLayout.getActionTypeOptionGroup()
                .getValue()).getActionType();
    }

    private void createRollout() {
        if (mandatoryCheck() && validateFields() && duplicateCheck()) {
            final Rollout rolloutToCreate = saveRollout();
            uiNotification.displaySuccess(i18n.get("message.save.success", new Object[] { rolloutToCreate.getName() }));
            eventBus.publish(this, RolloutEvent.CREATE_ROLLOUT);
            closeThisWindow();
        }
    }

    private Rollout saveRollout() {
        Rollout rolloutToCreate = new Rollout();
        final int amountGroup = Integer.parseInt(noOfGroups.getValue());
        final String targetFilter = getTargetFilterQuery();
        final int errorThresoldPercent = getErrorThresoldPercentage(amountGroup);

        final RolloutGroupConditions conditions = new RolloutGroup.RolloutGroupConditionBuilder()
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

    private boolean validateFields() {
        if (!noOfGroups.isValid() || !errorThreshold.isValid() || !triggerThreshold.isValid()) {
            uiNotification.displayValidationError(i18n.get("message.correct.invalid.value"));
            return false;
        }
        return true;
    }

    private void closeThisWindow() {
        addUpdateRolloutWindow.close();
        UI.getCurrent().removeWindow(addUpdateRolloutWindow);
    }

    private boolean mandatoryCheck() {
        final DistributionSetIdName ds = getDistributionSetSelected();
        final String targetFilter = (String) targetFilterQueryCombo.getValue();
        final String triggerThresoldValue = triggerThreshold.getValue();
        final String errorThresoldValue = errorThreshold.getValue();
        if (hasNoNameOrTargetFilter(targetFilter) || ds == null
                || HawkbitCommonUtil.trimAndNullIfEmpty(noOfGroups.getValue()) == null
                || isThresholdValueMissing(triggerThresoldValue, errorThresoldValue)) {
            uiNotification.displayValidationError(i18n.get("message.mandatory.check"));
            return false;
        }
        return true;
    }

    private boolean mandatoryCheckForEdit() {
        final DistributionSetIdName ds = getDistributionSetSelected();
        final String targetFilter = targetFilterQuery.getValue();
        final String triggerThresoldValue = triggerThreshold.getValue();
        final String errorThresoldValue = errorThreshold.getValue();
        if (hasNoNameOrTargetFilter(targetFilter) || ds == null
                || HawkbitCommonUtil.trimAndNullIfEmpty(noOfGroups.getValue()) == null
                || isThresholdValueMissing(triggerThresoldValue, errorThresoldValue)) {
            uiNotification.displayValidationError(i18n.get("message.mandatory.check"));
            return false;
        }
        return true;
    }

    private boolean hasNoNameOrTargetFilter(final String targetFilter) {
        return getRolloutName() == null || targetFilter == null;
    }

    private boolean isThresholdValueMissing(final String triggerThresoldValue, final String errorThresoldValue) {
        return HawkbitCommonUtil.trimAndNullIfEmpty(triggerThresoldValue) == null
                || HawkbitCommonUtil.trimAndNullIfEmpty(errorThresoldValue) == null;
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
        description = SPUIComponentProvider.getTextArea(i18n.get("textfield.description"), "text-area-style",
                ValoTheme.TEXTFIELD_TINY, false, null, i18n.get("textfield.description"),
                SPUILabelDefinitions.TEXT_AREA_MAX_LENGTH);
        description.setId(SPUIComponetIdProvider.ROLLOUT_DESCRIPTION_ID);
        description.setNullRepresentation(HawkbitCommonUtil.SP_STRING_EMPTY);
        description.setSizeFull();
        return description;
    }

    private void createErrorThreshold() {
        errorThreshold = SPUIComponentProvider.getTextField(null, "", ValoTheme.TEXTFIELD_TINY, false, null,
                i18n.get("prompt.error.threshold"), true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        errorThreshold.addValidator(new ThresholdFieldValidator());
        errorThreshold.setId(SPUIComponetIdProvider.ROLLOUT_ERROR_THRESOLD_ID);
        errorThreshold.setMaxLength(7);
        errorThreshold.setSizeFull();
    }

    private void createTriggerThresold() {
        triggerThreshold = SPUIComponentProvider.getTextField(null, "", ValoTheme.TEXTFIELD_TINY, false, null,
                i18n.get("prompt.tigger.thresold"), true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        triggerThreshold.setId(SPUIComponetIdProvider.ROLLOUT_TRIGGER_THRESOLD_ID);
        triggerThreshold.addValidator(new ThresholdFieldValidator());
        triggerThreshold.setSizeFull();
        triggerThreshold.setMaxLength(3);
    }

    private void createNoOfGroupsField() {
        noOfGroups = SPUIComponentProvider.getTextField(null, "", ValoTheme.TEXTFIELD_TINY, false, null,
                i18n.get("prompt.number.of.groups"), true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        noOfGroups.setId(SPUIComponetIdProvider.ROLLOUT_NO_OF_GROUPS_ID);
        noOfGroups.addValidator(new GroupNumberValidator());
        noOfGroups.setSizeFull();
        noOfGroups.setMaxLength(3);
        noOfGroups.addValueChangeListener(evevt -> onGroupNumberChange());
    }

    private void onGroupNumberChange() {
        if (noOfGroups.isValid() && !Strings.isNullOrEmpty(noOfGroups.getValue())) {
            groupSizeLabel.setValue(getTargetPerGroupMessage(String.valueOf(getGroupSize())));
            groupSizeLabel.setVisible(true);
        } else {
            groupSizeLabel.setVisible(false);
        }
    }

    private ComboBox createDistributionSetCombo() {
        distributionSet = SPUIComponentProvider.getComboBox(i18n.get("prompt.distribution.set"), "", "", null, null,
                true, "", i18n.get("prompt.distribution.set"));
        distributionSet.setImmediate(true);
        distributionSet.setPageLength(7);
        distributionSet.setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME);
        distributionSet.setId(SPUIComponetIdProvider.ROLLOUT_DS_ID);
        distributionSet.setSizeFull();
        return distributionSet;
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
        rolloutName = SPUIComponentProvider.getTextField(i18n.get("textfield.name"), "", ValoTheme.TEXTFIELD_TINY, true,
                null, i18n.get("textfield.name"), true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        rolloutName.setId(SPUIComponetIdProvider.ROLLOUT_NAME_FIELD_ID);
        rolloutName.setSizeFull();

        return rolloutName;
    }

    private Label createMandatoryLabel() {
        final Label madatoryLbl = new Label(i18n.get("label.mandatory.field"));
        madatoryLbl.addStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR + " " + ValoTheme.LABEL_SMALL);
        return madatoryLbl;
    }

    private String getRolloutName() {
        return HawkbitCommonUtil.trimAndNullIfEmpty(rolloutName.getValue());
    }

    private DistributionSetIdName getDistributionSetSelected() {
        return (DistributionSetIdName) distributionSet.getValue();
    }

    class ErrorThresoldOptionValidator implements Validator {
        private static final long serialVersionUID = 9049939751976326550L;

        @Override
        public void validate(final Object value) {
            try {
                if (HawkbitCommonUtil.trimAndNullIfEmpty(noOfGroups.getValue()) == null
                        || HawkbitCommonUtil.trimAndNullIfEmpty((String) targetFilterQueryCombo.getValue()) == null) {
                    uiNotification
                            .displayValidationError(i18n.get("message.rollout.noofgroups.or.targetfilter.missing"));
                } else {
                    new RegexpValidator(NUMBER_REGEXP, i18n.get(MESSAGE_ENTER_NUMBER)).validate(value);
                    final int groupSize = getGroupSize();
                    new IntegerRangeValidator(i18n.get(MESSAGE_ROLLOUT_FIELD_VALUE_RANGE, 0, groupSize), 0, groupSize)
                            .validate(Integer.valueOf(value.toString()));
                }
            } catch (final InvalidValueException ex) {
                throw ex;
            }
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
                new RegexpValidator(NUMBER_REGEXP, i18n.get(MESSAGE_ENTER_NUMBER)).validate(value);
                new IntegerRangeValidator(i18n.get(MESSAGE_ROLLOUT_FIELD_VALUE_RANGE, 0, 100), 0, 100)
                        .validate(Integer.valueOf(value.toString()));
            } catch (final InvalidValueException ex) {
                throw ex;
            }
        }
    }

    class GroupNumberValidator implements Validator {
        private static final long serialVersionUID = 9049939751976326550L;

        @Override
        public void validate(final Object value) {
            try {
                new RegexpValidator(NUMBER_REGEXP, i18n.get(MESSAGE_ENTER_NUMBER)).validate(value);
                new IntegerRangeValidator(i18n.get(MESSAGE_ROLLOUT_FIELD_VALUE_RANGE, 0, 500), 0, 500)
                        .validate(Integer.valueOf(value.toString()));
            } catch (final InvalidValueException ex) {
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
    public void populateData(final Long rolloutId) {
        resetComponents();
        editRolloutEnabled = Boolean.TRUE;
        rolloutForEdit = rolloutManagement.findRolloutById(rolloutId);
        rolloutName.setValue(rolloutForEdit.getName());
        description.setValue(rolloutForEdit.getDescription());
        distributionSet.setValue(rolloutForEdit.getDistributionSet().getDistributionSetIdName());
        final List<RolloutGroup> rolloutGroups = rolloutForEdit.getRolloutGroups();
        setThresoldValues(rolloutGroups);
        setActionType(rolloutForEdit);
        if (rolloutForEdit.getStatus() != RolloutStatus.READY) {
            disableRequiredFieldsOnEdit();
        }

        noOfGroups.setEnabled(false);
        targetFilterQuery.setValue(rolloutForEdit.getTargetFilterQuery());
        targetFilterQuery.setVisible(true);
        targetFilterQueryCombo.setVisible(false);

        totalTargetsCount = targetManagement.countTargetByTargetFilterQuery(rolloutForEdit.getTargetFilterQuery());
        totalTargetsLabel.setValue(getTotalTargetMessage());
        totalTargetsLabel.setVisible(true);
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
    private void setThresoldValues(final List<RolloutGroup> rolloutGroups) {
        if (null != rolloutGroups && !rolloutGroups.isEmpty()) {
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
