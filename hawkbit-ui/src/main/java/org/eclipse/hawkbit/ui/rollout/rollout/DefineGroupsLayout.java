/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rollout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.builder.RolloutGroupCreate;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.RolloutGroupsValidation;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.ui.common.builder.ComboBoxBuilder;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextAreaBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleNoBorderWithIcon;
import org.eclipse.hawkbit.ui.filtermanagement.TargetFilterBeanQuery;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Validator;
import com.vaadin.data.util.converter.StringToFloatConverter;
import com.vaadin.data.util.converter.StringToIntegerConverter;
import com.vaadin.data.validator.FloatRangeValidator;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.UserError;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;

/**
 * Define groups for a Rollout
 */
public class DefineGroupsLayout extends GridLayout {

    private static final long serialVersionUID = 2939193468001472916L;

    private final VaadinMessageSource i18n;

    private transient EntityFactory entityFactory;

    private transient RolloutManagement rolloutManagement;

    private transient RolloutGroupManagement rolloutGroupManagement;

    private final transient QuotaManagement quotaManagement;

    private transient TargetFilterQueryManagement targetFilterQueryManagement;

    private String defaultTriggerThreshold;

    private String defaultErrorThreshold;

    private String targetFilter;

    private transient List<GroupRow> groupRows;

    private int groupsCount;

    private transient List<RolloutGroupCreate> savedRolloutGroups;

    private transient ValidationListener validationListener;

    private ValidationStatus validationStatus = ValidationStatus.VALID;

    private transient RolloutGroupsValidation groupsValidation;

    private final AtomicInteger runningValidationsCounter;

    DefineGroupsLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final RolloutManagement rolloutManagement, final TargetFilterQueryManagement targetFilterQueryManagement,
            final RolloutGroupManagement rolloutGroupManagement, final QuotaManagement quotaManagement) {
        this.i18n = i18n;
        this.entityFactory = entityFactory;
        this.rolloutManagement = rolloutManagement;
        this.rolloutGroupManagement = rolloutGroupManagement;
        this.quotaManagement = quotaManagement;
        this.targetFilterQueryManagement = targetFilterQueryManagement;
        runningValidationsCounter = new AtomicInteger(0);

        groupRows = new ArrayList<>(10);
        setSizeUndefined();
        buildLayout();

    }

    private void buildLayout() {

        setSpacing(Boolean.TRUE);
        setSizeUndefined();
        setRows(3);
        setColumns(6);
        setStyleName("marginTop");

        addComponent(getLabel("caption.rollout.group.definition.desc"), 0, 0, 5, 0);

        final int headerRow = 1;
        addComponent(getLabel("header.name"), 0, headerRow);
        addComponent(getLabel("header.target.filter.query"), 1, headerRow);
        addComponent(getLabel("header.target.percentage"), 2, headerRow);
        addComponent(getLabel("header.rolloutgroup.threshold"), 3, headerRow);
        addComponent(getLabel("header.rolloutgroup.threshold.error"), 4, headerRow);

        addComponent(createAddButton(), 0, 2, 5, 2);

    }

    /**
     * @param targetFilter
     *            the target filter which is required for verification
     */
    public void setTargetFilter(final String targetFilter) {
        this.targetFilter = targetFilter;
        updateValidation();
    }

    private int addRow() {
        final int insertIndex = getRows() - 1;
        insertRow(insertIndex);
        return insertIndex;
    }

    private Label getLabel(final String key) {
        return new LabelBuilder().name(i18n.getMessage(key)).buildLabel();
    }

    private Button createAddButton() {
        final Button button = SPUIComponentProvider.getButton(UIComponentIdProvider.ROLLOUT_GROUP_ADD_ID,
                i18n.getMessage("button.rollout.add.group"), "", "", true, FontAwesome.PLUS,
                SPUIButtonStyleNoBorderWithIcon.class);
        button.setSizeUndefined();
        button.addStyleName("default-color");
        button.setEnabled(true);
        button.setVisible(true);
        button.addClickListener(event -> addGroupRowAndValidate());
        return button;

    }

    private GroupRow addGroupRow() {
        final int rowIndex = addRow();
        final GroupRow groupRow = new GroupRow();
        groupRow.addToGridRow(this, rowIndex);
        groupRows.add(groupRow);
        return groupRow;
    }

    private GroupRow addGroupRowAndValidate() {
        final GroupRow groupRow = addGroupRow();
        updateValidation();
        return groupRow;
    }

    public List<RolloutGroupCreate> getSavedRolloutGroups() {
        return savedRolloutGroups;
    }

    /**
     * @return the validation instance if was already validated
     */
    public RolloutGroupsValidation getGroupsValidation() {
        return groupsValidation;
    }

    private void removeAllRows() {
        for (int i = getRows() - 2; i > 1; i--) {
            removeRow(i);
        }

        groupRows.clear();
    }

    public void setDefaultTriggerThreshold(final String defaultTriggerThreshold) {
        this.defaultTriggerThreshold = defaultTriggerThreshold;
    }

    public void setDefaultErrorThreshold(final String defaultErrorThreshold) {
        this.defaultErrorThreshold = defaultErrorThreshold;
    }

    /**
     * Reset the field values.
     */
    public void resetComponents() {

        validationStatus = ValidationStatus.VALID;
        groupsCount = 0;

        removeAllRows();
        addGroupRowAndValidate();
    }

    /**
     * Populate groups by rollout
     *
     * @param rollout
     *            the rollout
     */
    public void populateByRollout(final Rollout rollout) {
        if (rollout == null) {
            return;
        }

        removeAllRows();

        final List<RolloutGroup> groups = rolloutGroupManagement
                .findByRollout(new PageRequest(0, quotaManagement.getMaxRolloutGroupsPerRollout()), rollout.getId())
                .getContent();
        for (final RolloutGroup group : groups) {
            final GroupRow groupRow = addGroupRow();
            groupRow.populateByGroup(group);
        }

        updateValidation();

    }

    /**
     * @return whether the groups definition form is valid
     */
    public boolean isValid() {
        if (groupRows.isEmpty() || validationStatus != ValidationStatus.VALID) {
            return false;
        }
        return groupRows.stream().allMatch(GroupRow::isValid);
    }

    private void updateValidation() {
        validationStatus = ValidationStatus.VALID;
        if (isValid()) {
            setValidationStatus(ValidationStatus.LOADING);
            savedRolloutGroups = getGroupsFromRows();
            validateRemainingTargets();

        } else {
            resetRemainingTargetsError();
            setValidationStatus(ValidationStatus.INVALID);
        }
    }

    private void setValidationStatus(final ValidationStatus status) {
        validationStatus = status;
        if (validationListener != null) {
            validationListener.validation(status);
        }
    }

    private void resetRemainingTargetsError() {
        groupRows.forEach(GroupRow::hideLastGroupError);
    }

    private void validateRemainingTargets() {
        resetRemainingTargetsError();
        if (targetFilter == null) {
            return;
        }

        if (runningValidationsCounter.incrementAndGet() == 1) {
            final ListenableFuture<RolloutGroupsValidation> validateTargetsInGroups = rolloutManagement
                    .validateTargetsInGroups(savedRolloutGroups, targetFilter, System.currentTimeMillis());
            final UI ui = UI.getCurrent();
            validateTargetsInGroups.addCallback(validation -> ui.access(() -> setGroupsValidation(validation)),
                    throwable -> ui.access(() -> setGroupsValidation(null)));
            return;
        }

        runningValidationsCounter.incrementAndGet();

    }

    /**
     * YOU SHOULD NOT CALL THIS METHOD MANUALLY. It's only for the callback.
     * Only 1 runningValidation should be executed. If this runningValidation is
     * done, then this method is called. Maybe then a new runningValidation is
     * executed.
     * 
     */
    private void setGroupsValidation(final RolloutGroupsValidation validation) {

        final int runningValidation = runningValidationsCounter.getAndSet(0);
        if (runningValidation > 1) {
            validateRemainingTargets();
            return;
        }
        groupsValidation = validation;

        final GroupRow lastRow = groupRows.get(groupRows.size() - 1);
        if (groupsValidation != null && groupsValidation.isValid() && validationStatus != ValidationStatus.INVALID) {
            lastRow.hideLastGroupError();
            setValidationStatus(ValidationStatus.VALID);

        } else {
            lastRow.markWithLastGroupError();
            setValidationStatus(ValidationStatus.INVALID);
        }

    }

    private List<RolloutGroupCreate> getGroupsFromRows() {
        return groupRows.stream().map(GroupRow::getGroupEntity).collect(Collectors.toList());
    }

    public void setValidationListener(final ValidationListener validationListener) {
        this.validationListener = validationListener;
    }

    /**
     * Status of the groups validation
     */
    public enum ValidationStatus {
        VALID, INVALID, LOADING
    }

    /**
     * Implement the interface and set the instance with setValidationListener
     * to receive updates for any changes within the group rows.
     */
    @FunctionalInterface
    public interface ValidationListener {
        /**
         * Is called after user input
         * 
         * @param isValid
         *            whether the input of the group rows is valid
         */
        void validation(ValidationStatus isValid);
    }

    private class GroupRow {

        private TextField groupName;

        private ComboBox targetFilterQueryCombo;

        private TextArea targetFilterQuery;

        private TextField targetPercentage;

        private TextField triggerThreshold;

        private TextField errorThreshold;

        private HorizontalLayout optionsLayout;

        private boolean populated;

        private boolean initialized;

        public GroupRow() {
            init();
        }

        private void init() {
            groupsCount += 1;
            groupName = createTextField("textfield.name", UIComponentIdProvider.ROLLOUT_GROUP_LIST_GRID_ID);
            groupName.setValue(i18n.getMessage("textfield.rollout.group.default.name", groupsCount));
            groupName.setStyleName("rollout-group-name");
            groupName.addValueChangeListener(

                    event -> valueChanged());

            targetFilterQueryCombo =

                    createTargetFilterQueryCombo();

            populateTargetFilterQuery();
            targetFilterQueryCombo.addValueChangeListener(event -> valueChanged());
            targetFilterQuery = createTargetFilterQuery();
            targetPercentage = createPercentageWithDecimalsField("textfield.target.percentage",
                    UIComponentIdProvider.ROLLOUT_GROUP_TARGET_PERC_ID);
            targetPercentage.setValue("100");
            targetPercentage.addValueChangeListener(event -> valueChanged());
            triggerThreshold = createPercentageField("prompt.tigger.threshold",
                    UIComponentIdProvider.ROLLOUT_TRIGGER_THRESOLD_ID);
            triggerThreshold.setValue(defaultTriggerThreshold);
            triggerThreshold.addValueChangeListener(event -> valueChanged());
            errorThreshold = createPercentageField("prompt.error.threshold",
                    UIComponentIdProvider.ROLLOUT_ERROR_THRESOLD_ID);
            errorThreshold.setValue(defaultErrorThreshold);
            errorThreshold.addValueChangeListener(event -> valueChanged());
            optionsLayout = new HorizontalLayout();
            optionsLayout.addComponent(createRemoveButton());
            initialized = true;
        }

        private TextField createTextField(final String in18Key, final String id) {
            final TextField textField = new TextFieldBuilder().prompt(i18n.getMessage(in18Key)).immediate(true).id(id)
                    .buildTextComponent();
            textField.setSizeUndefined();
            textField.addValidator(
                    new StringLengthValidator(i18n.getMessage("message.rollout.group.name.invalid"), 1, 64, false));
            return textField;
        }

        private TextField createPercentageField(final String in18Key, final String id) {
            final TextField textField = new TextFieldBuilder().prompt(i18n.getMessage(in18Key)).immediate(true).id(id)
                    .buildTextComponent();
            textField.setWidth(80, Unit.PIXELS);
            textField.setNullRepresentation("");
            textField.setConverter(new StringToIntegerConverter());
            textField.addValidator(this::validateMandatoryPercentage);
            return textField;
        }

        private TextField createPercentageWithDecimalsField(final String in18Key, final String id) {
            final TextField textField = createPercentageField(in18Key, id);
            textField.setConverter(new StringToFloatConverter());
            return textField;
        }

        private void removeGroupRow(final GroupRow groupRow) {
            groupRows.remove(groupRow);
            updateValidation();
        }

        private void validateMandatoryPercentage(final Object value) {
            if (value != null) {
                final String message = i18n.getMessage("message.rollout.field.value.range", 0, 100);
                if (value instanceof Float) {
                    new FloatRangeValidator(message, 0F, 100F).validate(value);
                }
                if (value instanceof Integer) {
                    new IntegerRangeValidator(message, 0, 100).validate(value);
                }
            } else {
                throw new Validator.EmptyValueException(i18n.getMessage("message.enter.number"));
            }
        }

        private void valueChanged() {
            if (initialized) {
                updateValidation();
            }
        }

        private ComboBox createTargetFilterQueryCombo() {
            return new ComboBoxBuilder().setId(UIComponentIdProvider.ROLLOUT_TARGET_FILTER_COMBO_ID)
                    .setPrompt(i18n.getMessage("prompt.target.filter")).buildCombBox();
        }

        private TextArea createTargetFilterQuery() {
            final TextArea filterField = new TextAreaBuilder().style("text-area-style")
                    .id(UIComponentIdProvider.ROLLOUT_TARGET_FILTER_QUERY_FIELD)
                    .maxLengthAllowed(SPUILabelDefinitions.TARGET_FILTER_QUERY_TEXT_FIELD_LENGTH).buildTextComponent();

            filterField.setNullRepresentation("");
            filterField.setEnabled(false);
            filterField.setSizeUndefined();
            return filterField;
        }

        private void populateTargetFilterQuery() {
            final Container container = createTargetFilterComboContainer();
            targetFilterQueryCombo.setContainerDataSource(container);
        }

        private void populateTargetFilterQuery(final RolloutGroup group) {
            if (StringUtils.isEmpty(group.getTargetFilterQuery())) {
                targetFilterQueryCombo.setValue(null);
            } else {
                final Page<TargetFilterQuery> filterQueries = targetFilterQueryManagement
                        .findByQuery(new PageRequest(0, 1), group.getTargetFilterQuery());
                if (filterQueries.getTotalElements() > 0) {
                    final TargetFilterQuery filterQuery = filterQueries.getContent().get(0);
                    targetFilterQueryCombo.setValue(filterQuery.getName());
                }
            }
        }

        private Container createTargetFilterComboContainer() {
            final BeanQueryFactory<TargetFilterBeanQuery> targetFilterQF = new BeanQueryFactory<>(
                    TargetFilterBeanQuery.class);
            return new LazyQueryContainer(
                    new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_NAME),
                    targetFilterQF);
        }

        private Button createRemoveButton() {
            final Button button = SPUIComponentProvider.getButton(UIComponentIdProvider.ROLLOUT_GROUP_REMOVE_ID, "", "",
                    "", true, FontAwesome.MINUS, SPUIButtonStyleNoBorderWithIcon.class);
            button.setSizeUndefined();
            button.addStyleName("default-color");
            button.setEnabled(true);
            button.setVisible(true);
            button.addClickListener(event -> onRemove());
            return button;
        }

        private void onRemove() {
            final int index = findRowIndexFor(groupName, 0);
            if (index != -1) {
                removeRow(index);
            }
            removeGroupRow(this);
        }

        private int findRowIndexFor(final Component component, final int col) {
            final int rows = getRows();
            for (int i = 0; i < rows; i++) {
                final Component rowComponent = getComponent(col, i);
                if (component.equals(rowComponent)) {
                    return i;
                }
            }
            return -1;
        }

        private String getTargetFilterQuery() {
            if (!StringUtils.hasText((String) targetFilterQueryCombo.getValue())) {
                return null;
            }
            final Item filterItem = targetFilterQueryCombo.getContainerDataSource()
                    .getItem(targetFilterQueryCombo.getValue());
            return (String) filterItem.getItemProperty("query").getValue();
        }

        /**
         * Adds this group row to a grid layout
         * 
         * @param layout
         *            the grid layout
         * @param rowIndex
         *            the row of the grid layout
         */
        public void addToGridRow(final GridLayout layout, final int rowIndex) {
            layout.addComponent(groupName, 0, rowIndex);
            if (populated) {
                layout.addComponent(targetFilterQuery, 1, rowIndex);
            } else {
                layout.addComponent(targetFilterQueryCombo, 1, rowIndex);
            }
            layout.addComponent(targetPercentage, 2, rowIndex);
            layout.addComponent(triggerThreshold, 3, rowIndex);
            layout.addComponent(errorThreshold, 4, rowIndex);
            layout.addComponent(optionsLayout, 5, rowIndex);

        }

        /**
         * Builds a group definition from this group row
         * 
         * @return the RolloutGroupCreate definition
         */
        public RolloutGroupCreate getGroupEntity() {
            final RolloutGroupConditionBuilder conditionBuilder = new RolloutGroupConditionBuilder()
                    .successAction(RolloutGroup.RolloutGroupSuccessAction.NEXTGROUP, null)
                    .successCondition(RolloutGroup.RolloutGroupSuccessCondition.THRESHOLD, triggerThreshold.getValue());
            if (!StringUtils.isEmpty(errorThreshold.getValue())) {
                conditionBuilder
                        .errorCondition(RolloutGroup.RolloutGroupErrorCondition.THRESHOLD, errorThreshold.getValue())
                        .errorAction(RolloutGroup.RolloutGroupErrorAction.PAUSE, null);
            }
            final String percentageString = targetPercentage.getValue().replace(",", ".");
            final Float percentage = Float.parseFloat(percentageString);

            return entityFactory.rolloutGroup().create().name(groupName.getValue()).description(groupName.getValue())
                    .targetFilterQuery(getTargetFilterQuery()).targetPercentage(percentage)
                    .conditions(conditionBuilder.build());
        }

        /**
         * Populates the row with the data from the provided groups.
         * 
         * @param group
         *            the data source
         */
        public void populateByGroup(final RolloutGroup group) {
            initialized = false;
            groupName.setValue(group.getName());
            targetFilterQuery.setValue(group.getTargetFilterQuery());
            populateTargetFilterQuery(group);

            targetPercentage.setValue(String.format("%.2f", group.getTargetPercentage()));
            triggerThreshold.setValue(group.getSuccessConditionExp());
            errorThreshold.setValue(group.getErrorConditionExp());

            populated = true;
            initialized = true;

        }

        /**
         * @return whether the data entered in this row is valid
         */
        public boolean isValid() {
            return !StringUtils.isEmpty(groupName.getValue()) && targetPercentage.isValid()
                    && triggerThreshold.isValid() && errorThreshold.isValid();
        }

        private void markWithLastGroupError() {
            targetPercentage
                    .setComponentError(new UserError(i18n.getMessage("message.rollout.remaining.targets.error")));
        }

        /**
         * Hides an error of the row
         */
        private void hideLastGroupError() {
            targetPercentage.setComponentError(null);
        }

    }

}
