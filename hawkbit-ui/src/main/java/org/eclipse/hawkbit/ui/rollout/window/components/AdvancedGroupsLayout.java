/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.window.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.builder.RolloutGroupCreate;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.RolloutGroupsValidation;
import org.eclipse.hawkbit.ui.common.data.mappers.AdvancedRolloutGroupDefinitionToCreateMapper;
import org.eclipse.hawkbit.ui.common.data.providers.TargetFilterQueryDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyAdvancedRolloutGroup;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleNoBorderWithIcon;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;

import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

/**
 * Define groups for a Rollout
 */
public class AdvancedGroupsLayout extends ValidatableLayout {
    private static final String MESSAGE_ROLLOUT_MAX_GROUP_SIZE_EXCEEDED = "message.rollout.max.group.size.exceeded.advanced";

    private final VaadinMessageSource i18n;
    private final EntityFactory entityFactory;
    private final RolloutManagement rolloutManagement;
    private final QuotaManagement quotaManagement;

    private final TargetFilterQueryDataProvider targetFilterQueryDataProvider;

    private final GridLayout layout;

    private String targetFilter;
    private Long dsTypeId;

    private final List<AdvancedGroupRow> groupRows;
    private int lastGroupIndex;

    private final AtomicInteger runningValidationsCounter;

    private BiConsumer<List<ProxyAdvancedRolloutGroup>, Boolean> advancedGroupDefinitionsChangedListener;

    final TenantConfigHelper tenantConfigHelper;
    
    /**
     * Constructor for AdvancedGroupsLayout
     *
     * @param i18n
     *            VaadinMessageSource
     * @param entityFactory
     *            EntityFactory
     * @param rolloutManagement
     *            RolloutManagement
     * @param quotaManagement
     *            QuotaManagement
     * @param targetFilterQueryDataProvider
     *            TargetFilterQueryDataProvider
     */
    public AdvancedGroupsLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final RolloutManagement rolloutManagement, final QuotaManagement quotaManagement,
            final TargetFilterQueryDataProvider targetFilterQueryDataProvider,
            final TenantConfigHelper tenantConfigHelper) {
        super();

        this.i18n = i18n;
        this.entityFactory = entityFactory;
        this.rolloutManagement = rolloutManagement;
        this.quotaManagement = quotaManagement;
        this.targetFilterQueryDataProvider = targetFilterQueryDataProvider;
        this.tenantConfigHelper = tenantConfigHelper;

        this.layout = buildLayout();

        this.groupRows = new ArrayList<>(10);
        this.runningValidationsCounter = new AtomicInteger(0);
    }

    private GridLayout buildLayout() {
        final GridLayout gridLayout = new GridLayout();
        gridLayout.setMargin(false);
        gridLayout.setSpacing(true);
        gridLayout.setSizeUndefined();
        gridLayout.setRows(3);
        if (tenantConfigHelper.isConfirmationFlowEnabled()) {
            gridLayout.setColumns(7);
        } else {
            gridLayout.setColumns(6);
        }
        gridLayout.setStyleName("marginTop");

        gridLayout.addComponent(SPUIComponentProvider.generateLabel(i18n, "caption.rollout.group.definition.desc"), 0,
                0, 5, 0);
        addHeaderRow(gridLayout, 1);
        gridLayout.addComponent(createAddButton(), 0, 2, 5, 2);

        return gridLayout;
    }

    private void addHeaderRow(final GridLayout gridLayout, final int headerRow) {
        final List<String> headerColumns = new ArrayList<>(Arrays.asList("header.name", "header.target.filter.query",
                "header.target.percentage", "header.rolloutgroup.threshold", "header.rolloutgroup.threshold.error"));
        if (tenantConfigHelper.isConfirmationFlowEnabled()) {
            headerColumns.add("header.rolloutgroup.confirmation");
        }
        for (int i = 0; i < headerColumns.size(); i++) {
            final Label label = SPUIComponentProvider.generateLabel(i18n, headerColumns.get(i));
            gridLayout.addComponent(label, i, headerRow);
        }
    }

    private Button createAddButton() {
        final Button button = SPUIComponentProvider.getButton(UIComponentIdProvider.ROLLOUT_GROUP_ADD_ID,
                i18n.getMessage("button.rollout.add.group"), "", "", true, VaadinIcons.PLUS,
                SPUIButtonStyleNoBorderWithIcon.class);
        button.setSizeUndefined();
        button.addStyleName("default-color");
        button.setEnabled(true);
        button.setVisible(true);
        button.addClickListener(event -> addGroupRowAndValidate());
        return button;

    }

    /**
     * Add advance group ro and validate
     */
    public void addGroupRowAndValidate() {
        addGroupRow(getDefaultAdvancedRolloutGroupDefinition());

        updateValidation();
    }

    private ProxyAdvancedRolloutGroup getDefaultAdvancedRolloutGroupDefinition() {
        final ProxyAdvancedRolloutGroup advancedGroupRowBean = new ProxyAdvancedRolloutGroup();
        advancedGroupRowBean.setGroupName(i18n.getMessage("textfield.rollout.group.default.name", lastGroupIndex + 1));
        advancedGroupRowBean.setTargetPercentage(100.0F);
        setDefaultThresholds(advancedGroupRowBean);

        return advancedGroupRowBean;
    }

    private static void setDefaultThresholds(final ProxyAdvancedRolloutGroup advancedGroupRow) {
        final RolloutGroupConditions defaultRolloutGroupConditions = new RolloutGroupConditionBuilder().withDefaults()
                .build();
        advancedGroupRow.setTriggerThresholdPercentage(defaultRolloutGroupConditions.getSuccessConditionExp());
        advancedGroupRow.setErrorThresholdPercentage(defaultRolloutGroupConditions.getErrorConditionExp());
    }

    private void addGroupRow(final ProxyAdvancedRolloutGroup advancedRolloutGroupDefinition) {
        final AdvancedGroupRow groupRow = addGroupRow();
        groupRow.setBean(advancedRolloutGroupDefinition);

        groupRow.addStatusChangeListener(event -> updateValidation());
    }

    private AdvancedGroupRow addGroupRow() {
        final AdvancedGroupRow groupRow = new AdvancedGroupRow(i18n, targetFilterQueryDataProvider,
                tenantConfigHelper.isConfirmationFlowEnabled());

        addRowToLayout(groupRow);
        groupRows.add(groupRow);

        lastGroupIndex++;
        groupRow.updateComponentIds(lastGroupIndex);

        return groupRow;
    }

    private void addRowToLayout(final AdvancedGroupRow groupRow) {
        final int index = layout.getRows() - 1;
        layout.insertRow(index);

        groupRow.addRowToLayout(layout, index);

        final int removeButtonColumnIndex = tenantConfigHelper.isConfirmationFlowEnabled() ? 6 : 5;

        layout.addComponent(createRemoveButton(groupRow, index), removeButtonColumnIndex, index);
    }

    private Button createRemoveButton(final AdvancedGroupRow groupRow, final int index) {
        final Button button = SPUIComponentProvider.getButton(
                UIComponentIdProvider.ROLLOUT_GROUP_REMOVE_ID + "." + index, "", "", "", true, VaadinIcons.MINUS,
                SPUIButtonStyleNoBorderWithIcon.class);
        button.setSizeUndefined();
        button.addStyleName("default-color");

        button.addClickListener(event -> removeGroupRow(groupRow, index));

        return button;
    }

    // there is limited amount of advanced groups within the rollout
    @SuppressWarnings("squid:S2250")
    private void removeGroupRow(final AdvancedGroupRow groupRow, final int index) {
        layout.removeRow(index);
        groupRows.remove(groupRow);

        updateValidation();
    }

    private void updateValidation() {
        if (!allGroupRowsValid()) {
            setValidationStatus(ValidationStatus.INVALID);
            if (advancedGroupDefinitionsChangedListener != null) {
                advancedGroupDefinitionsChangedListener.accept(Collections.emptyList(), false);
            }

            return;
        }

        setValidationStatus(ValidationStatus.UNKNOWN);
        if (advancedGroupDefinitionsChangedListener != null) {
            advancedGroupDefinitionsChangedListener.accept(Collections.emptyList(), true);
        }

        validateTargetsPerGroup();
    }

    private boolean allGroupRowsValid() {
        if (groupRows.isEmpty()) {
            return false;
        }

        return groupRows.stream().allMatch(AdvancedGroupRow::isValid);
    }

    private void validateTargetsPerGroup() {
        resetErrors();

        if (StringUtils.isEmpty(targetFilter) || dsTypeId == null) {
            return;
        }

        if (runningValidationsCounter.incrementAndGet() == 1) {
            final List<RolloutGroupCreate> groupsCreate = getRolloutGroupsCreateFromDefinitions(
                    getAdvancedRolloutGroupDefinitions());
            final ListenableFuture<RolloutGroupsValidation> validateTargetsInGroups = rolloutManagement
                    .validateTargetsInGroups(groupsCreate, targetFilter, System.currentTimeMillis(), dsTypeId);

            final UI ui = UI.getCurrent();
            validateTargetsInGroups.addCallback(validation -> ui.access(() -> updateGroupsByValidation(validation)),
                    throwable -> ui.access(() -> updateGroupsByValidation(null)));
        }
    }

    private void resetErrors() {
        groupRows.forEach(AdvancedGroupRow::resetError);
    }

    /**
     * @return List of advance rollout group
     */
    public List<ProxyAdvancedRolloutGroup> getAdvancedRolloutGroupDefinitions() {
        return groupRows.stream().map(AdvancedGroupRow::getBean).collect(Collectors.toList());
    }

    private List<RolloutGroupCreate> getRolloutGroupsCreateFromDefinitions(
            final List<ProxyAdvancedRolloutGroup> advancedRolloutGroupDefinitions) {
        final AdvancedRolloutGroupDefinitionToCreateMapper mapper = new AdvancedRolloutGroupDefinitionToCreateMapper(
                entityFactory);

        return advancedRolloutGroupDefinitions.stream().map(mapper::map).collect(Collectors.toList());
    }

    /**
     * YOU SHOULD NOT CALL THIS METHOD MANUALLY. It's only for the callback. Only 1
     * runningValidation should be executed. If this runningValidation is done, then
     * this method is called. Maybe then a new runningValidation is executed.
     * 
     */
    private void updateGroupsByValidation(final RolloutGroupsValidation validation) {
        final int runningValidation = runningValidationsCounter.getAndSet(0);
        if (runningValidation > 1) {
            validateTargetsPerGroup();
            return;
        }

        if (validation == null || CollectionUtils.isEmpty(validation.getTargetsPerGroup())) {
            setValidationStatus(ValidationStatus.INVALID);
            return;
        }

        final List<Long> targetsPerGroup = validation.getTargetsPerGroup();
        updateGroupsTargetCount(targetsPerGroup);

        final boolean isTargetsPerGroupQuotaExceeded = targetsPerGroup.stream()
                .anyMatch(this::isGroupTargetQuotaExceeded);
        final boolean hasRemainingTargets = !validation.isValid();

        if (isTargetsPerGroupQuotaExceeded || hasRemainingTargets) {
            if (hasRemainingTargets) {
                final AdvancedGroupRow lastRow = groupRows.get(groupRows.size() - 1);
                lastRow.setError(i18n.getMessage("message.rollout.remaining.targets.error"));
            }

            setValidationStatus(ValidationStatus.INVALID);
            return;
        }

        setValidationStatus(ValidationStatus.VALID);
    }

    private void updateGroupsTargetCount(final List<Long> targetsPerGroup) {
        for (int i = 0; i < groupRows.size(); ++i) {
            final AdvancedGroupRow row = groupRows.get(i);
            final Long targetsCount = targetsPerGroup.get(i);

            row.getBean().setTargetsCount(targetsCount);
            if (isGroupTargetQuotaExceeded(targetsCount)) {
                row.setError(i18n.getMessage(MESSAGE_ROLLOUT_MAX_GROUP_SIZE_EXCEEDED,
                        quotaManagement.getMaxTargetsPerRolloutGroup()));
            }
        }

        if (advancedGroupDefinitionsChangedListener != null) {
            advancedGroupDefinitionsChangedListener.accept(getAdvancedRolloutGroupDefinitions(), false);
        }
    }

    private boolean isGroupTargetQuotaExceeded(final Long targetsCount) {
        return targetsCount != null && targetsCount > quotaManagement.getMaxTargetsPerRolloutGroup();
    }

    /**
     * @param targetFilter
     *            the target filter which is required for verification
     */
    public void setTargetFilter(final String targetFilter) {
        this.targetFilter = targetFilter;
        updateValidation();
    }

    /**
     * 
     * @param dsTypeId
     *            ID of the Distribution set type which is required for the
     *            compatibility check
     */
    public void setDsTypeId(final Long dsTypeId) {
        this.dsTypeId = dsTypeId;
        updateValidation();
    }

    /**
     * @param targetFilter
     *            the target filter which is required for verification
     * @param dsTypeId
     *            ID of the Distribution set type which is required for the
     *            compatibility check
     */
    public void setTargetFilterAndDsType(final String targetFilter, final Long dsTypeId) {
        this.targetFilter = targetFilter;
        this.dsTypeId = dsTypeId;
        updateValidation();
    }

    /**
     * Populate groups by rollout groups
     *
     * @param groups
     *            the rollout groups
     */
    public void populateByAdvancedRolloutGroupDefinitions(final List<ProxyAdvancedRolloutGroup> groups) {
        if (CollectionUtils.isEmpty(groups)) {
            return;
        }

        removeAllRows();
        groups.forEach(this::addGroupRow);

        updateValidation();
    }

    private void removeAllRows() {
        for (int i = layout.getRows() - 2; i > 1; i--) {
            layout.removeRow(i);
        }

        groupRows.clear();
        lastGroupIndex = 0;
    }

    /**
     * @return Advance group layout
     */
    public GridLayout getLayout() {
        return layout;
    }

    /**
     * Sets the event change listener in rollout advance group definitions
     *
     * @param advancedGroupDefinitionsChangedListener
     *            Event change listener
     */
    public void setAdvancedGroupDefinitionsChangedListener(
            final BiConsumer<List<ProxyAdvancedRolloutGroup>, Boolean> advancedGroupDefinitionsChangedListener) {
        this.advancedGroupDefinitionsChangedListener = advancedGroupDefinitionsChangedListener;
    }
}
