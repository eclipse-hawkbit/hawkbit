/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.window.layouts;

import java.util.Arrays;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyAdvancedRolloutGroup;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRolloutWindow;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRolloutWindow.GroupDefinitionMode;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetFilterQuery;
import org.eclipse.hawkbit.ui.rollout.window.RolloutWindowDependencies;
import org.eclipse.hawkbit.ui.rollout.window.components.AdvancedGroupsLayout;
import org.eclipse.hawkbit.ui.rollout.window.components.RolloutFormLayout;
import org.eclipse.hawkbit.ui.rollout.window.components.SimpleGroupsLayout;
import org.eclipse.hawkbit.ui.rollout.window.components.VisualGroupDefinitionLayout;
import org.springframework.util.StringUtils;

import com.vaadin.data.ValidationException;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.TabSheet;

/**
 * Layout builder for Add Rollout window together with component value change
 * listeners.
 */
@SuppressWarnings({ "squid:MaximumInheritanceDepth", "squid:S2160" })
public class AddRolloutWindowLayout extends AbstractRolloutWindowLayout {

    private final TargetManagement targetManagement;
    private final DistributionSetManagement dsManagement;

    private final RolloutFormLayout rolloutFormLayout;
    private final SimpleGroupsLayout simpleGroupsLayout;
    private final AdvancedGroupsLayout advancedGroupsLayout;
    private final TabSheet groupsDefinitionTabs;
    private final VisualGroupDefinitionLayout visualGroupDefinitionLayout;

    private String filterQuery;
    private Long dsTypeId;
    private Long totalTargets;
    private int noOfGroups;
    private List<ProxyAdvancedRolloutGroup> advancedRolloutGroupDefinitions;

    /**
     * Constructor for AddRolloutWindowLayout
     *
     * @param dependencies
     *            RolloutWindowDependencies
     */
    public AddRolloutWindowLayout(final RolloutWindowDependencies dependencies) {
        super(dependencies);

        this.targetManagement = dependencies.getTargetManagement();
        this.dsManagement = dependencies.getDistributionSetManagement();

        this.rolloutFormLayout = rolloutComponentBuilder.createRolloutFormLayout();
        this.simpleGroupsLayout = rolloutComponentBuilder.createSimpleGroupsLayout();
        this.advancedGroupsLayout = rolloutComponentBuilder.createAdvancedGroupsLayout();
        this.groupsDefinitionTabs = rolloutComponentBuilder.createGroupDefinitionTabs(simpleGroupsLayout.getLayout(),
                advancedGroupsLayout.getLayout());
        this.visualGroupDefinitionLayout = rolloutComponentBuilder.createVisualGroupDefinitionLayout();

        addValueChangeListeners();

        addValidatableLayouts(Arrays.asList(rolloutFormLayout, simpleGroupsLayout));
    }

    @Override
    protected void addComponents(final GridLayout rootLayout) {
        rootLayout.setRows(7);

        final int lastRowIdx = rootLayout.getRows() - 1;
        final int lastColumnIdx = rootLayout.getColumns() - 1;

        rolloutFormLayout.addFormToAddLayout(rootLayout);
        visualGroupDefinitionLayout.addChartWithLegendToLayout(rootLayout, lastColumnIdx, 3);
        rootLayout.addComponent(groupsDefinitionTabs, 0, lastRowIdx, lastColumnIdx, lastRowIdx);
    }

    private void addValueChangeListeners() {
        rolloutFormLayout.setFilterQueryChangedListener(this::onFilterQueryChange);
        rolloutFormLayout.setDistSetChangedListener(this::onDistSetTypeChange);
        groupsDefinitionTabs.addSelectedTabChangeListener(event -> onGroupDefinitionTabChanged());
        simpleGroupsLayout.setNoOfGroupsChangedListener(this::onNoOfSimpleGroupsChanged);
        advancedGroupsLayout.setAdvancedGroupDefinitionsChangedListener(this::onAdvancedGroupsChanged);
    }

    private void onFilterQueryChange(final ProxyTargetFilterQuery targetFilterQuery) {
        filterQuery = targetFilterQuery != null ? targetFilterQuery.getQuery() : null;
        updateTotalTargets();

        if (isAdvancedGroupsTabSelected()) {
            advancedGroupsLayout.setTargetFilter(filterQuery);
        }
    }

    private void onDistSetTypeChange(final ProxyDistributionSet ds) {
        dsTypeId = ds != null ? getDsTypeId(ds) : null;
        updateTotalTargets();

        if (isAdvancedGroupsTabSelected()) {
            advancedGroupsLayout.setDsTypeId(dsTypeId);
        }
    }

    private Long getDsTypeId(final @NotNull ProxyDistributionSet ds) {
        if (ds.getTypeInfo() != null) {
            return ds.getTypeInfo().getId();
        }

        return dsManagement.get(ds.getId()).map(dist -> dist.getType().getId()).orElse(null);
    }

    private void updateTotalTargets() {
        totalTargets = getTotalTargets(filterQuery, dsTypeId);
        rolloutFormLayout.setTotalTargets(totalTargets);
        visualGroupDefinitionLayout.setTotalTargets(totalTargets);
        if (isSimpleGroupsTabSelected()) {
            simpleGroupsLayout.setTotalTargets(totalTargets);
        }
    }

    private Long getTotalTargets(final String filterQuery, final Long distSetTypeId) {
        if (StringUtils.isEmpty(filterQuery)) {
            return null;
        }
        if (distSetTypeId == null) {
            return targetManagement.countByRsql(filterQuery);
        }
        return targetManagement.countByRsqlAndCompatible(filterQuery, distSetTypeId);
    }

    private boolean isSimpleGroupsTabSelected() {
        return groupsDefinitionTabs.getSelectedTab().equals(simpleGroupsLayout.getLayout());
    }

    private boolean isAdvancedGroupsTabSelected() {
        return groupsDefinitionTabs.getSelectedTab().equals(advancedGroupsLayout.getLayout());
    }

    private void onGroupDefinitionTabChanged() {
        if (isSimpleGroupsTabSelected()) {
            adaptSimpleGroupsValidation();

            simpleGroupsLayout.setTotalTargets(totalTargets);

            visualGroupDefinitionLayout.setGroupDefinitionMode(GroupDefinitionMode.SIMPLE);
            visualGroupDefinitionLayout.setNoOfGroups(noOfGroups);
        }

        if (isAdvancedGroupsTabSelected()) {
            adaptAdvancedGroupsValidation();

            advancedGroupsLayout.setTargetFilterAndDsType(filterQuery, dsTypeId);

            visualGroupDefinitionLayout.setGroupDefinitionMode(GroupDefinitionMode.ADVANCED);
            visualGroupDefinitionLayout.setAdvancedRolloutGroupDefinitions(advancedRolloutGroupDefinitions);
        }
    }

    private void adaptSimpleGroupsValidation() {
        advancedGroupsLayout.resetValidationStatus();
        removeValidatableLayout(advancedGroupsLayout);

        addValidatableLayout(simpleGroupsLayout);
    }

    private void adaptAdvancedGroupsValidation() {
        simpleGroupsLayout.resetValidationStatus();
        removeValidatableLayout(simpleGroupsLayout);

        addValidatableLayout(advancedGroupsLayout);
    }

    private void onNoOfSimpleGroupsChanged(final int noOfGroups) {
        if (!isSimpleGroupsTabSelected()) {
            return;
        }

        this.noOfGroups = noOfGroups;

        visualGroupDefinitionLayout.setNoOfGroups(noOfGroups);
    }

    private void onAdvancedGroupsChanged(final List<ProxyAdvancedRolloutGroup> advancedRolloutGroupDefinitions,
            final Boolean isLoading) {
        if (!isAdvancedGroupsTabSelected()) {
            return;
        }

        this.advancedRolloutGroupDefinitions = advancedRolloutGroupDefinitions;

        visualGroupDefinitionLayout.setAdvancedRolloutGroupDefinitions(advancedRolloutGroupDefinitions);

        if (isLoading) {
            visualGroupDefinitionLayout.displayLoading();
        }
    }

    /**
     * Add advance group row and validate
     */
    public void addAdvancedGroupRowAndValidate() {
        advancedGroupsLayout.addGroupRowAndValidate();
    }

    /**
     * Select advance group tab in the add rollout window
     */
    public void selectAdvancedGroupsTab() {
        groupsDefinitionTabs.setSelectedTab(advancedGroupsLayout.getLayout());
    }

    @Override
    public void setEntity(final ProxyRolloutWindow proxyEntity) {
        rolloutFormLayout.setBean(proxyEntity.getRolloutForm());
        simpleGroupsLayout.setBean(proxyEntity.getSimpleGroupsDefinition());
        advancedGroupsLayout
                .populateByAdvancedRolloutGroupDefinitions(proxyEntity.getAdvancedRolloutGroupDefinitions());
        visualGroupDefinitionLayout.setGroupDefinitionMode(proxyEntity.getGroupDefinitionMode());
    }

    @Override
    public ProxyRolloutWindow getValidatableEntity() throws ValidationException {
        final ProxyRolloutWindow proxyEntity = new ProxyRolloutWindow();
        proxyEntity.setRolloutForm(rolloutFormLayout.getBean());

        if (isSimpleGroupsTabSelected()) {
            proxyEntity.setSimpleGroupsDefinition(simpleGroupsLayout.getBean());
            proxyEntity.setGroupDefinitionMode(GroupDefinitionMode.SIMPLE);
        } else {
            proxyEntity.setAdvancedRolloutGroupDefinitions(advancedGroupsLayout.getAdvancedRolloutGroupDefinitions());
            proxyEntity.setGroupDefinitionMode(GroupDefinitionMode.ADVANCED);
        }
        return proxyEntity;
    }
}
