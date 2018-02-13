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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.builder.RolloutCreate;
import org.eclipse.hawkbit.repository.builder.RolloutGroupCreate;
import org.eclipse.hawkbit.repository.builder.RolloutUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.RepositoryModelConstants;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.RolloutGroupsValidation;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow.SaveDialogCloseListener;
import org.eclipse.hawkbit.ui.common.builder.ComboBoxBuilder;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextAreaBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.common.builder.WindowBuilder;
import org.eclipse.hawkbit.ui.filtermanagement.TargetFilterBeanQuery;
import org.eclipse.hawkbit.ui.management.footer.ActionTypeOptionGroupLayout;
import org.eclipse.hawkbit.ui.management.footer.ActionTypeOptionGroupLayout.ActionTypeOption;
import org.eclipse.hawkbit.ui.rollout.event.RolloutEvent;
import org.eclipse.hawkbit.ui.rollout.groupschart.GroupsPieChart;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.StringUtils;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Validator;
import com.vaadin.data.util.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.data.validator.LongRangeValidator;
import com.vaadin.data.validator.NullValidator;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Rollout add or update popup layout.
 */
public class AddUpdateRolloutWindowLayout extends GridLayout {

    private static final long serialVersionUID = 2999293468801479916L;

    private static final Logger LOGGER = LoggerFactory.getLogger(AddUpdateRolloutWindowLayout.class);

    private static final String MESSAGE_ROLLOUT_FIELD_VALUE_RANGE = "message.rollout.field.value.range";

    private static final String MESSAGE_ROLLOUT_FILTER_TARGET_EXISTS = "message.rollout.filter.target.exists";

    private static final String MESSAGE_ENTER_NUMBER = "message.enter.number";

    private final ActionTypeOptionGroupLayout actionTypeOptionGroupLayout;

    private final AutoStartOptionGroupLayout autoStartOptionGroupLayout;

    private final transient RolloutManagement rolloutManagement;

    private final transient RolloutGroupManagement rolloutGroupManagement;

    private final transient QuotaManagement quotaManagement;

    private final transient TargetManagement targetManagement;

    private final transient TargetFilterQueryManagement targetFilterQueryManagement;

    private final UINotification uiNotification;

    private final transient UiProperties uiProperties;

    private final transient EntityFactory entityFactory;

    private final DefineGroupsLayout defineGroupsLayout;

    private final VaadinMessageSource i18n;

    private final transient EventBus.UIEventBus eventBus;

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

    private boolean editRolloutEnabled;

    private Rollout rollout;

    private Long totalTargetsCount;

    private TextArea targetFilterQuery;

    private TabSheet groupsDefinitionTabs;

    private GroupsPieChart groupsPieChart;

    private GroupsLegendLayout groupsLegendLayout;

    private final transient RolloutGroupConditions defaultRolloutGroupConditions;

    private final NullValidator nullValidator = new NullValidator(null, false);

    AddUpdateRolloutWindowLayout(final RolloutManagement rolloutManagement, final TargetManagement targetManagement,
            final UINotification uiNotification, final UiProperties uiProperties, final EntityFactory entityFactory,
            final VaadinMessageSource i18n, final UIEventBus eventBus,
            final TargetFilterQueryManagement targetFilterQueryManagement,
            final RolloutGroupManagement rolloutGroupManagement, final QuotaManagement quotaManagement) {
        actionTypeOptionGroupLayout = new ActionTypeOptionGroupLayout(i18n);
        autoStartOptionGroupLayout = new AutoStartOptionGroupLayout(i18n);
        this.rolloutManagement = rolloutManagement;
        this.rolloutGroupManagement = rolloutGroupManagement;
        this.quotaManagement = quotaManagement;
        this.targetManagement = targetManagement;
        this.uiNotification = uiNotification;
        this.uiProperties = uiProperties;
        this.entityFactory = entityFactory;
        this.i18n = i18n;
        this.eventBus = eventBus;
        this.targetFilterQueryManagement = targetFilterQueryManagement;

        defineGroupsLayout = new DefineGroupsLayout(i18n, entityFactory, rolloutManagement, targetFilterQueryManagement,
                rolloutGroupManagement, quotaManagement);

        defaultRolloutGroupConditions = new RolloutGroupConditionBuilder().withDefaults().build();

        setSizeUndefined();
        createRequiredComponents();
        buildLayout();

        defineGroupsLayout.setValidationListener(this::displayValidationStatus);
        defineGroupsLayout.setDefaultErrorThreshold(defaultRolloutGroupConditions.getErrorConditionExp());
        defineGroupsLayout.setDefaultTriggerThreshold(defaultRolloutGroupConditions.getSuccessConditionExp());
    }

    /**
     * Save or update the rollout.
     */
    private final class SaveOnDialogCloseListener implements SaveDialogCloseListener {

        @Override
        public void saveOrUpdate() {

            if (editRolloutEnabled) {
                editRollout();
                return;
            }
            createRollout();
        }

        @Override
        public boolean canWindowSaveOrUpdate() {
            if (editRolloutEnabled) {
                return duplicateCheckForEdit();
            }
            return duplicateCheck();
        }

        private void createRollout() {
            final Rollout rolloutToCreate = saveRollout();
            uiNotification.displaySuccess(i18n.getMessage("message.save.success", rolloutToCreate.getName()));
        }

        private boolean duplicateCheck() {
            if (!StringUtils.hasText(rolloutName.getValue())) {
                uiNotification.displayValidationError(i18n.getMessage("message.rollout.name.empty"));
                return false;
            }
            if (rolloutManagement.getByName(getRolloutName()).isPresent()) {
                uiNotification
                        .displayValidationError(i18n.getMessage("message.rollout.duplicate.check", getRolloutName()));
                return false;
            }
            return true;
        }

        private void editRollout() {
            if (rollout == null) {
                return;
            }

            final Long distributionSetId = (Long) distributionSet.getValue();

            final RolloutUpdate rolloutUpdate = entityFactory.rollout().update(rollout.getId())
                    .name(rolloutName.getValue()).description(description.getValue()).set(distributionSetId)
                    .actionType(getActionType()).forcedTime(getForcedTimeStamp());

            if (AutoStartOptionGroupLayout.AutoStartOption.AUTO_START.equals(getAutoStartOption())) {
                rolloutUpdate.startAt(System.currentTimeMillis());
            }
            if (AutoStartOptionGroupLayout.AutoStartOption.SCHEDULED.equals(getAutoStartOption())) {
                rolloutUpdate.startAt(getScheduledStartTime());
            }

            Rollout updatedRollout;
            try {
                updatedRollout = rolloutManagement.update(rolloutUpdate);
            } catch (final EntityNotFoundException | EntityReadOnlyException e) {
                LOGGER.warn("Rollout was deleted. Redirect to Rollouts overview.", e);
                uiNotification.displayWarning(
                        "Rollout with name " + rolloutName.getValue() + " was deleted. Update is not poosible");
                eventBus.publish(this, RolloutEvent.SHOW_ROLLOUTS);
                return;
            }

            uiNotification.displaySuccess(i18n.getMessage("message.update.success", updatedRollout.getName()));
            eventBus.publish(this, RolloutEvent.UPDATE_ROLLOUT);
        }

        private boolean duplicateCheckForEdit() {
            if (!StringUtils.hasText(rolloutName.getValue())) {
                uiNotification.displayValidationError(i18n.getMessage("message.rollout.name.empty"));
                return false;
            }
            final String rolloutNameVal = getRolloutName();
            if (!rollout.getName().equals(rolloutNameVal) && rolloutManagement.getByName(rolloutNameVal).isPresent()) {
                uiNotification
                        .displayValidationError(i18n.getMessage("message.rollout.duplicate.check", rolloutNameVal));
                return false;
            }
            return true;
        }

        private String getRolloutName() {
            return StringUtils.trimWhitespace(rolloutName.getValue());
        }

        private Rollout saveRollout() {

            final Long distributionId = (Long) distributionSet.getValue();

            final int amountGroup = Integer.parseInt(noOfGroups.getValue());
            final int errorThresholdPercent = getErrorThresholdPercentage(amountGroup);
            final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder()
                    .successAction(RolloutGroupSuccessAction.NEXTGROUP, null)
                    .successCondition(RolloutGroupSuccessCondition.THRESHOLD, triggerThreshold.getValue())
                    .errorCondition(RolloutGroupErrorCondition.THRESHOLD, String.valueOf(errorThresholdPercent))
                    .errorAction(RolloutGroupErrorAction.PAUSE, null).build();

            final RolloutCreate rolloutCreate = entityFactory.rollout().create().name(rolloutName.getValue())
                    .description(description.getValue()).set(distributionId).targetFilterQuery(getTargetFilterQuery())
                    .actionType(getActionType()).forcedTime(getForcedTimeStamp());

            if (AutoStartOptionGroupLayout.AutoStartOption.AUTO_START.equals(getAutoStartOption())) {
                rolloutCreate.startAt(System.currentTimeMillis());
            }
            if (AutoStartOptionGroupLayout.AutoStartOption.SCHEDULED.equals(getAutoStartOption())) {
                rolloutCreate.startAt(getScheduledStartTime());
            }

            if (isNumberOfGroups()) {
                return rolloutManagement.create(rolloutCreate, amountGroup, conditions);
            } else if (isGroupsDefinition()) {
                final List<RolloutGroupCreate> groups = defineGroupsLayout.getSavedRolloutGroups();
                return rolloutManagement.create(rolloutCreate, groups, conditions);
            }

            throw new IllegalStateException("Either of the Tabs must be selected");
        }

        private long getForcedTimeStamp() {
            return ActionTypeOption.AUTO_FORCED
                    .equals(actionTypeOptionGroupLayout.getActionTypeOptionGroup().getValue())
                            ? actionTypeOptionGroupLayout.getForcedTimeDateField().getValue().getTime()
                            : RepositoryModelConstants.NO_FORCE_TIME;
        }

        private Long getScheduledStartTime() {
            return AutoStartOptionGroupLayout.AutoStartOption.SCHEDULED.equals(getAutoStartOption())
                    ? autoStartOptionGroupLayout.getStartAtDateField().getValue().getTime() : null;
        }

        private int getErrorThresholdPercentage(final int amountGroup) {
            int errorThresoldPercent = Integer.parseInt(errorThreshold.getValue());
            if (errorThresholdOptionGroup.getValue().equals(ERRORTHRESOLDOPTIONS.COUNT.getValue())) {
                final int groupSize = (int) Math.ceil((double) totalTargetsCount / (double) amountGroup);
                final int erroThresoldCount = Integer.parseInt(errorThreshold.getValue());
                errorThresoldPercent = (int) Math.ceil(((float) erroThresoldCount / (float) groupSize) * 100);
            }
            return errorThresoldPercent;
        }

        private ActionType getActionType() {
            return ((ActionTypeOptionGroupLayout.ActionTypeOption) actionTypeOptionGroupLayout
                    .getActionTypeOptionGroup().getValue()).getActionType();
        }

        private AutoStartOptionGroupLayout.AutoStartOption getAutoStartOption() {
            return (AutoStartOptionGroupLayout.AutoStartOption) autoStartOptionGroupLayout.getAutoStartOptionGroup()
                    .getValue();
        }

    }

    CommonDialogWindow getWindow(final Long rolloutId, final boolean copy) {
        resetComponents();
        window = createWindow();
        populateData(rolloutId, copy);
        return window;
    }

    private CommonDialogWindow createWindow() {
        return new WindowBuilder(SPUIDefinitions.CREATE_UPDATE_WINDOW)
                .caption(i18n.getMessage("caption.configure.rollout")).content(this).layout(this).i18n(i18n)
                .helpLink(uiProperties.getLinks().getDocumentation().getRolloutView())
                .saveDialogCloseListener(new SaveOnDialogCloseListener()).buildCommonDialogWindow();
    }

    public CommonDialogWindow getWindow() {
        resetComponents();
        window = createWindow();
        window.updateAllComponents(noOfGroups);
        window.updateAllComponents(triggerThreshold);
        window.updateAllComponents(errorThreshold);
        return window;
    }

    /**
     * Reset the field values.
     */
    private void resetComponents() {
        defineGroupsLayout.resetComponents();
        editRolloutEnabled = false;
        rolloutName.clear();
        targetFilterQuery.clear();
        resetFields();
        enableFields();
        populateDistributionSet();
        populateTargetFilterQuery();
        setDefaultSaveStartGroupOption();
        groupsLegendLayout.reset();
        groupSizeLabel.setVisible(false);
        noOfGroups.setVisible(true);
        removeComponent(1, 2);
        addComponent(targetFilterQueryCombo, 1, 2);
        addGroupsLegendLayout();
        addGroupsDefinitionTabs();

        actionTypeOptionGroupLayout.selectDefaultOption();
        autoStartOptionGroupLayout.selectDefaultOption();
        totalTargetsCount = 0L;
        rollout = null;
        groupsDefinitionTabs.setVisible(true);
        groupsDefinitionTabs.setSelectedTab(0);
    }

    private void addGroupsDefinitionTabs() {
        if (getComponent(0, 6) == null) {
            addComponent(groupsDefinitionTabs, 0, 6, 3, 6);
        }
    }

    private void addGroupsLegendLayout() {
        if (getComponent(3, 0) == null) {
            addComponent(groupsLegendLayout, 3, 0, 3, 3);
        }
    }

    private void resetFields() {
        rolloutName.removeStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
        noOfGroups.clear();
        noOfGroups.removeStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
        triggerThreshold.setValue(defaultRolloutGroupConditions.getSuccessConditionExp());
        triggerThreshold.removeStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
        errorThreshold.setValue(defaultRolloutGroupConditions.getErrorConditionExp());
        errorThreshold.removeStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
        description.clear();
        description.removeStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
    }

    private void buildLayout() {

        setSpacing(true);
        setSizeUndefined();
        setRows(7);
        setColumns(4);
        setStyleName("marginTop");
        setColumnExpandRatio(3, 1);
        setWidth(850, Unit.PIXELS);

        addComponent(getMandatoryLabel("textfield.name"), 0, 0);
        addComponent(rolloutName, 1, 0);
        rolloutName.addValidator(nullValidator);

        addComponent(getMandatoryLabel("prompt.distribution.set"), 0, 1);
        addComponent(distributionSet, 1, 1);
        distributionSet.addValidator(nullValidator);

        addComponent(getMandatoryLabel("prompt.target.filter"), 0, 2);
        addComponent(targetFilterQueryCombo, 1, 2);
        targetFilterQueryCombo.addValidator(nullValidator);
        targetFilterQueryCombo.addValidator(new TargetExistsValidator());
        targetFilterQuery.removeValidator(nullValidator);

        addComponent(getLabel("textfield.description"), 0, 3);
        addComponent(description, 1, 3, 1, 3);

        addComponent(groupsLegendLayout, 3, 0, 3, 3);
        addComponent(groupsPieChart, 2, 0, 2, 3);

        addComponent(getMandatoryLabel("caption.rollout.action.type"), 0, 4);
        addComponent(actionTypeOptionGroupLayout, 1, 4, 3, 4);

        addComponent(getMandatoryLabel("caption.rollout.start.type"), 0, 5);
        addComponent(autoStartOptionGroupLayout, 1, 5, 3, 5);

        addComponent(groupsDefinitionTabs, 0, 6, 3, 6);

        rolloutName.focus();
    }

    private Label getMandatoryLabel(final String key) {
        final Label mandatoryLabel = getLabel(i18n.getMessage(key));
        mandatoryLabel.setContentMode(ContentMode.HTML);
        mandatoryLabel.setValue(mandatoryLabel.getValue().concat(" <span style='color:#ed473b'>*</span>"));
        return mandatoryLabel;
    }

    private Label getLabel(final String key) {
        return new LabelBuilder().name(i18n.getMessage(key)).buildLabel();
    }

    private TextField createTextField(final String in18Key, final String id) {
        return new TextFieldBuilder().prompt(i18n.getMessage(in18Key)).immediate(true).id(id).buildTextComponent();
    }

    private TextField createIntegerTextField(final String in18Key, final String id) {
        final TextField textField = createTextField(in18Key, id);
        textField.setNullRepresentation("");
        textField.setConverter(new StringToIntegerConverter());
        textField.setConversionError(i18n.getMessage(MESSAGE_ENTER_NUMBER));
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
        autoStartOptionGroupLayout.selectDefaultOption();
        targetFilterQuery = createTargetFilterQuery();
        actionTypeOptionGroupLayout.addStyleName(SPUIStyleDefinitions.ROLLOUT_ACTION_TYPE_LAYOUT);
        autoStartOptionGroupLayout.addStyleName(SPUIStyleDefinitions.ROLLOUT_ACTION_TYPE_LAYOUT);

        groupsDefinitionTabs = createGroupDefinitionTabs();

        groupsPieChart = new GroupsPieChart();
        groupsPieChart.setWidth(260, Unit.PIXELS);
        groupsPieChart.setHeight(220, Unit.PIXELS);
        groupsPieChart.setStyleName(SPUIStyleDefinitions.ROLLOUT_GROUPS_CHART);

        groupsLegendLayout = new GroupsLegendLayout(i18n);

    }

    private void displayValidationStatus(final DefineGroupsLayout.ValidationStatus status) {
        if (status == DefineGroupsLayout.ValidationStatus.LOADING) {
            groupsLegendLayout.displayLoading();
        } else {
            validateGroups();
        }
    }

    private TabSheet createGroupDefinitionTabs() {
        final TabSheet tabSheet = new TabSheet();
        tabSheet.setId(UIComponentIdProvider.ROLLOUT_GROUPS);
        tabSheet.setWidth(850, Unit.PIXELS);
        tabSheet.setHeight(300, Unit.PIXELS);
        tabSheet.setStyleName(SPUIStyleDefinitions.ROLLOUT_GROUPS);

        final TabSheet.Tab simpleTab = tabSheet.addTab(createSimpleGroupDefinitionTab(),
                i18n.getMessage("caption.rollout.tabs.simple"));
        simpleTab.setId(UIComponentIdProvider.ROLLOUT_SIMPLE_TAB);

        final TabSheet.Tab advancedTab = tabSheet.addTab(defineGroupsLayout,
                i18n.getMessage("caption.rollout.tabs.advanced"));
        advancedTab.setId(UIComponentIdProvider.ROLLOUT_ADVANCED_TAB);

        tabSheet.addSelectedTabChangeListener(event -> validateGroups());

        return tabSheet;
    }

    private static int getPositionOfSelectedTab(final TabSheet tabSheet) {
        return tabSheet.getTabPosition(tabSheet.getTab(tabSheet.getSelectedTab()));
    }

    private boolean isNumberOfGroups() {
        return getPositionOfSelectedTab(groupsDefinitionTabs) == 0;
    }

    private boolean isGroupsDefinition() {
        return getPositionOfSelectedTab(groupsDefinitionTabs) == 1;
    }

    private GridLayout createSimpleGroupDefinitionTab() {
        final GridLayout layout = new GridLayout();
        layout.setSpacing(true);
        layout.setColumns(3);
        layout.setRows(4);
        layout.setStyleName("marginTop");

        layout.addComponent(getLabel("caption.rollout.generate.groups"), 0, 0, 2, 0);

        layout.addComponent(getMandatoryLabel("prompt.number.of.groups"), 0, 1);
        layout.addComponent(noOfGroups, 1, 1);
        noOfGroups.addValidator(nullValidator);
        layout.addComponent(groupSizeLabel, 2, 1);

        layout.addComponent(getMandatoryLabel("prompt.tigger.threshold"), 0, 2);
        layout.addComponent(triggerThreshold, 1, 2);
        triggerThreshold.addValidator(nullValidator);
        layout.addComponent(getPercentHintLabel(), 2, 2);

        layout.addComponent(getMandatoryLabel("prompt.error.threshold"), 0, 3);
        layout.addComponent(errorThreshold, 1, 3);
        errorThreshold.addValidator(nullValidator);
        layout.addComponent(errorThresholdOptionGroup, 2, 3);

        return layout;
    }

    private static Label createCountLabel() {
        final Label groupSize = new LabelBuilder().visible(false).name("").buildLabel();
        groupSize.addStyleName(ValoTheme.LABEL_TINY + " " + "rollout-target-count-message");
        groupSize.setImmediate(true);
        groupSize.setSizeUndefined();
        return groupSize;
    }

    private static TextArea createTargetFilterQuery() {
        final TextArea filterField = new TextAreaBuilder().style("text-area-style")
                .id(UIComponentIdProvider.ROLLOUT_TARGET_FILTER_QUERY_FIELD)
                .maxLengthAllowed(SPUILabelDefinitions.TARGET_FILTER_QUERY_TEXT_FIELD_LENGTH).buildTextComponent();

        filterField.setId(UIComponentIdProvider.ROLLOUT_TARGET_FILTER_QUERY_FIELD);
        filterField.setNullRepresentation("");
        filterField.setEnabled(false);
        filterField.setSizeUndefined();
        return filterField;
    }

    private OptionGroup createErrorThresholdOptionGroup() {
        final OptionGroup errorThresoldOptions = new OptionGroup();
        for (final ERRORTHRESOLDOPTIONS option : ERRORTHRESOLDOPTIONS.values()) {
            errorThresoldOptions.addItem(option.getValue());
        }
        errorThresoldOptions.setId(UIComponentIdProvider.ROLLOUT_ERROR_THRESOLD_OPTION_ID);
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

    private void validateGroups() {
        if (editRolloutEnabled) {
            return;
        }
        if (isGroupsDefinition()) {
            final List<RolloutGroupCreate> savedRolloutGroups = defineGroupsLayout.getSavedRolloutGroups();
            if (!defineGroupsLayout.isValid() || savedRolloutGroups == null || savedRolloutGroups.isEmpty()) {
                noOfGroups.clear();
            } else {
                noOfGroups.setValue(String.valueOf(savedRolloutGroups.size()));
            }
            updateGroupsChart(defineGroupsLayout.getGroupsValidation());
        }
        if (isNumberOfGroups()) {
            if (noOfGroups.isValid() && !noOfGroups.getValue().isEmpty()) {
                updateGroupsChart(Integer.parseInt(noOfGroups.getValue()));
            } else {
                updateGroupsChart(0);
            }
        }
    }

    private void updateGroupsChart(final RolloutGroupsValidation validation) {
        if (validation == null) {
            groupsPieChart.setChartState(null, null);
            return;
        }
        final List<Long> targetsPerGroup = validation.getTargetsPerGroup();
        if (validation.getTotalTargets() == 0L || targetsPerGroup.isEmpty()) {
            groupsPieChart.setChartState(null, null);
        } else {
            groupsPieChart.setChartState(targetsPerGroup, validation.getTotalTargets());
        }

        totalTargetsCount = validation.getTotalTargets();
        groupsLegendLayout.populateTotalTargets(validation.getTotalTargets());
        groupsLegendLayout.populateGroupsLegendByValidation(validation, defineGroupsLayout.getSavedRolloutGroups());

    }

    private void updateGroupsChart(final List<RolloutGroup> savedGroups, final long totalTargetsCount) {
        final List<Long> targetsPerGroup = savedGroups.stream().map(group -> (long) group.getTotalTargets())
                .collect(Collectors.toList());

        groupsPieChart.setChartState(targetsPerGroup, totalTargetsCount);
        groupsLegendLayout.populateGroupsLegendByGroups(savedGroups);
    }

    private void updateGroupsChart(final int amountOfGroups) {
        if (totalTargetsCount == null || totalTargetsCount == 0L || amountOfGroups == 0) {
            groupsPieChart.setChartState(null, null);
            groupsLegendLayout.populateGroupsLegendByTargetCounts(Collections.emptyList());
        } else {
            final List<Long> groups = new ArrayList<>(amountOfGroups);
            long leftTargets = totalTargetsCount;
            for (int i = 0; i < amountOfGroups; i++) {
                final float percentage = 1.0F / (amountOfGroups - i);
                final long targetsInGroup = Math.round(percentage * (double) leftTargets);
                leftTargets -= targetsInGroup;
                groups.add(targetsInGroup);
            }

            groupsPieChart.setChartState(groups, totalTargetsCount);
            groupsLegendLayout.populateGroupsLegendByTargetCounts(groups);
        }
    }

    private ComboBox createTargetFilterQueryCombo() {
        return new ComboBoxBuilder().setValueChangeListener(this::onTargetFilterChange)
                .setPrompt(i18n.getMessage("prompt.target.filter"))
                .setId(UIComponentIdProvider.ROLLOUT_TARGET_FILTER_COMBO_ID).buildCombBox();
    }

    private void onTargetFilterChange(final ValueChangeEvent event) {
        final String filterQueryString = getTargetFilterQuery();
        if (StringUtils.isEmpty(filterQueryString)) {
            totalTargetsCount = 0L;
            groupsLegendLayout.populateTotalTargets(null);
            defineGroupsLayout.setTargetFilter(null);
        } else {
            totalTargetsCount = targetManagement.countByRsql(filterQueryString);
            groupsLegendLayout.populateTotalTargets(totalTargetsCount);
            defineGroupsLayout.setTargetFilter(filterQueryString);
        }
        onGroupNumberChange(event);
    }

    private String getTargetPerGroupMessage(final String value) {
        return new StringBuilder(i18n.getMessage("label.target.per.group")).append(value).toString();
    }

    private void populateTargetFilterQuery() {
        final Container container = createTargetFilterComboContainer();
        targetFilterQueryCombo.setContainerDataSource(container);
    }

    private void populateTargetFilterQuery(final Rollout rollout) {
        final Page<TargetFilterQuery> filterQueries = targetFilterQueryManagement.findByQuery(new PageRequest(0, 1),
                rollout.getTargetFilterQuery());
        if (filterQueries.getTotalElements() > 0) {
            final TargetFilterQuery filterQuery = filterQueries.getContent().get(0);
            targetFilterQueryCombo.setValue(filterQuery.getName());
        }
    }

    private static Container createTargetFilterComboContainer() {
        final BeanQueryFactory<TargetFilterBeanQuery> targetFilterQF = new BeanQueryFactory<>(
                TargetFilterBeanQuery.class);
        return new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_NAME),
                targetFilterQF);
    }

    private String getTargetFilterQuery() {
        if (StringUtils.isEmpty(targetFilterQueryCombo.getValue())) {
            return null;
        }
        final Item filterItem = targetFilterQueryCombo.getContainerDataSource()
                .getItem(targetFilterQueryCombo.getValue());
        return (String) filterItem.getItemProperty("query").getValue();
    }

    private void setDefaultSaveStartGroupOption() {
        errorThresholdOptionGroup.setValue(ERRORTHRESOLDOPTIONS.PERCENT.getValue());
    }

    private TextArea createDescription() {
        final TextArea descriptionField = new TextAreaBuilder().style("text-area-style")
                .prompt(i18n.getMessage("textfield.description")).id(UIComponentIdProvider.ROLLOUT_DESCRIPTION_ID)
                .buildTextComponent();
        descriptionField.setNullRepresentation("");
        descriptionField.setSizeUndefined();
        return descriptionField;
    }

    private TextField createErrorThreshold() {
        final TextField errorField = createIntegerTextField("prompt.error.threshold",
                UIComponentIdProvider.ROLLOUT_ERROR_THRESOLD_ID);
        errorField.addValidator(new ThresholdFieldValidator());
        errorField.setMaxLength(7);
        errorField.setValue(defaultRolloutGroupConditions.getErrorConditionExp());
        return errorField;
    }

    private TextField createTriggerThreshold() {
        final TextField thresholdField = createIntegerTextField("prompt.tigger.threshold",
                UIComponentIdProvider.ROLLOUT_TRIGGER_THRESOLD_ID);
        thresholdField.addValidator(new ThresholdFieldValidator());
        thresholdField.setValue(defaultRolloutGroupConditions.getSuccessConditionExp());
        return thresholdField;
    }

    private TextField createNoOfGroupsField() {
        final TextField noOfGroupsField = createIntegerTextField("prompt.number.of.groups",
                UIComponentIdProvider.ROLLOUT_NO_OF_GROUPS_ID);
        noOfGroupsField.addValidator(new GroupNumberValidator());
        noOfGroupsField.setMaxLength(3);
        noOfGroupsField.addValueChangeListener(this::onGroupNumberChange);
        return noOfGroupsField;
    }

    private void onGroupNumberChange(final ValueChangeEvent event) {
        if (editRolloutEnabled) {
            return;
        }
        if (event.getProperty().getValue() != null && noOfGroups.isValid() && totalTargetsCount != null
                && isNumberOfGroups()) {
            groupSizeLabel.setValue(getTargetPerGroupMessage(String.valueOf(getGroupSize())));
            groupSizeLabel.setVisible(true);
            updateGroupsChart(Integer.parseInt(noOfGroups.getValue()));
        } else {
            groupSizeLabel.setVisible(false);
            if (isNumberOfGroups()) {
                updateGroupsChart(0);
            }
        }
    }

    private ComboBox createDistributionSetCombo() {
        return new ComboBoxBuilder().setPrompt(i18n.getMessage("prompt.distribution.set"))
                .setId(UIComponentIdProvider.ROLLOUT_DS_ID).buildCombBox();
    }

    private void populateDistributionSet() {
        final Container container = createDsComboContainer();
        distributionSet.setContainerDataSource(container);
    }

    private static Container createDsComboContainer() {
        final BeanQueryFactory<DistributionBeanQuery> distributionQF = new BeanQueryFactory<>(
                DistributionBeanQuery.class);
        return new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_ID), distributionQF);
    }

    private TextField createRolloutNameField() {
        final TextField rolloutNameField = createTextField("textfield.name",
                UIComponentIdProvider.ROLLOUT_NAME_FIELD_ID);
        rolloutNameField.setSizeUndefined();
        return rolloutNameField;
    }

    class ErrorThresoldOptionValidator implements Validator {
        private static final long serialVersionUID = 9049939751976326550L;

        @Override
        public void validate(final Object value) {
            if (isNoOfGroupsOrTargetFilterEmpty()) {
                uiNotification
                        .displayValidationError(i18n.getMessage("message.rollout.noofgroups.or.targetfilter.missing"));
            } else {
                if (value != null) {
                    final int groupSize = getGroupSize();
                    new IntegerRangeValidator(i18n.getMessage(MESSAGE_ROLLOUT_FIELD_VALUE_RANGE, 0, groupSize), 0,
                            groupSize).validate(Integer.valueOf(value.toString()));
                }
            }
        }

        private boolean isNoOfGroupsOrTargetFilterEmpty() {
            return !StringUtils.hasText(noOfGroups.getValue())
                    || (!StringUtils.hasText((String) targetFilterQueryCombo.getValue())
                            && !StringUtils.hasText(targetFilterQuery.getValue()));
        }
    }

    private int getGroupSize() {
        return (int) Math.ceil((double) totalTargetsCount / Double.parseDouble(noOfGroups.getValue()));
    }

    class TargetExistsValidator implements Validator {
        private static final long serialVersionUID = 1L;

        @Override
        public void validate(final Object value) {
            if (value != null) {
                new LongRangeValidator(i18n.getMessage(MESSAGE_ROLLOUT_FILTER_TARGET_EXISTS), 1L, null)
                        .validate(totalTargetsCount);
            }
        }
    }

    class ThresholdFieldValidator implements Validator {
        private static final long serialVersionUID = 9049939751976326550L;

        @Override
        public void validate(final Object value) {
            if (value != null) {
                new IntegerRangeValidator(i18n.getMessage(MESSAGE_ROLLOUT_FIELD_VALUE_RANGE, 0, 100), 0, 100)
                        .validate(Integer.valueOf(value.toString()));
            }
        }
    }

    class GroupNumberValidator implements Validator {
        private static final long serialVersionUID = 9043919751971326521L;

        @Override
        public void validate(final Object value) {
            if (value != null) {
                new IntegerRangeValidator(i18n.getMessage(MESSAGE_ROLLOUT_FIELD_VALUE_RANGE, 1, 500), 1, 500)
                        .validate(Integer.valueOf(value.toString()));
            }
        }
    }

    private void populateData(final Long rolloutId, final boolean copy) {
        if (rolloutId == null) {
            return;
        }

        final Optional<Rollout> rolloutFound = rolloutManagement.get(rolloutId);
        if (!rolloutFound.isPresent()) {
            return;
        }

        rollout = rolloutFound.get();
        description.setValue(rollout.getDescription());
        distributionSet.setValue(rollout.getDistributionSet().getId());
        setActionType(rollout);
        setAutoStartType(rollout);

        if (copy) {
            rolloutName.setValue(i18n.getMessage("textfield.rollout.copied.name", rollout.getName()));
            populateTargetFilterQuery(rollout);

            defineGroupsLayout.populateByRollout(rollout);
            groupsDefinitionTabs.setSelectedTab(1);

            window.clearOriginalValues();
        } else {
            editRolloutEnabled = true;
            if (rollout.getStatus() != Rollout.RolloutStatus.READY) {
                disableRequiredFieldsOnEdit();
            }
            rolloutName.setValue(rollout.getName());
            groupsDefinitionTabs.setVisible(false);

            targetFilterQuery.setValue(rollout.getTargetFilterQuery());
            removeComponent(1, 2);
            targetFilterQueryCombo.removeValidator(nullValidator);
            addComponent(targetFilterQuery, 1, 2);
            targetFilterQuery.addValidator(nullValidator);

            removeComponent(defineGroupsLayout);
            removeComponent(groupsDefinitionTabs);

            window.updateAllComponents(this);
            window.setOrginaleValues();

            updateGroupsChart(rolloutGroupManagement
                    .findByRollout(new PageRequest(0, quotaManagement.getMaxRolloutGroupsPerRollout()), rollout.getId())
                    .getContent(), rollout.getTotalTargets());
        }

        totalTargetsCount = targetManagement.countByRsql(rollout.getTargetFilterQuery());
        groupsLegendLayout.populateTotalTargets(totalTargetsCount);
    }

    private void disableRequiredFieldsOnEdit() {
        noOfGroups.setEnabled(false);
        distributionSet.setEnabled(false);
        errorThreshold.setEnabled(false);
        triggerThreshold.setEnabled(false);
        errorThresholdOptionGroup.setEnabled(false);
        actionTypeOptionGroupLayout.getActionTypeOptionGroup().setEnabled(false);
        actionTypeOptionGroupLayout.addStyleName(SPUIStyleDefinitions.DISABLE_ACTION_TYPE_LAYOUT);
        autoStartOptionGroupLayout.getAutoStartOptionGroup().setEnabled(false);
        autoStartOptionGroupLayout.addStyleName(SPUIStyleDefinitions.DISABLE_ACTION_TYPE_LAYOUT);
    }

    private void enableFields() {
        distributionSet.setEnabled(true);
        errorThreshold.setEnabled(true);
        triggerThreshold.setEnabled(true);
        actionTypeOptionGroupLayout.getActionTypeOptionGroup().setEnabled(true);
        actionTypeOptionGroupLayout.removeStyleName(SPUIStyleDefinitions.DISABLE_ACTION_TYPE_LAYOUT);
        autoStartOptionGroupLayout.getAutoStartOptionGroup().setEnabled(true);
        autoStartOptionGroupLayout.removeStyleName(SPUIStyleDefinitions.DISABLE_ACTION_TYPE_LAYOUT);
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

    private void setAutoStartType(final Rollout rollout) {
        if (rollout.getStartAt() == null) {
            autoStartOptionGroupLayout.getAutoStartOptionGroup()
                    .setValue(AutoStartOptionGroupLayout.AutoStartOption.MANUAL);
        } else if (rollout.getStartAt() < System.currentTimeMillis()) {
            autoStartOptionGroupLayout.getAutoStartOptionGroup()
                    .setValue(AutoStartOptionGroupLayout.AutoStartOption.AUTO_START);
            autoStartOptionGroupLayout.getStartAtDateField().setValue(new Date(rollout.getStartAt()));
        } else {
            autoStartOptionGroupLayout.getAutoStartOptionGroup()
                    .setValue(AutoStartOptionGroupLayout.AutoStartOption.SCHEDULED);
            autoStartOptionGroupLayout.getStartAtDateField().setValue(new Date(rollout.getStartAt()));
        }
    }

    private enum ERRORTHRESOLDOPTIONS {
        PERCENT("%"), COUNT("Count");

        String value;

        ERRORTHRESOLDOPTIONS(final String val) {
            value = val;
        }

        private String getValue() {
            return value;
        }
    }
}
