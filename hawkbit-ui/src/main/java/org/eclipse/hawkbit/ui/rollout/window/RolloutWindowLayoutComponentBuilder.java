/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.window;

import org.eclipse.hawkbit.ui.common.data.mappers.DistributionSetToProxyDistributionMapper;
import org.eclipse.hawkbit.ui.common.data.mappers.TargetFilterQueryToProxyTargetFilterMapper;
import org.eclipse.hawkbit.ui.common.data.providers.DistributionSetStatelessDataProvider;
import org.eclipse.hawkbit.ui.common.data.providers.TargetFilterQueryDataProvider;
import org.eclipse.hawkbit.ui.rollout.groupschart.GroupsPieChart;
import org.eclipse.hawkbit.ui.rollout.window.components.AdvancedGroupsLayout;
import org.eclipse.hawkbit.ui.rollout.window.components.ApprovalLayout;
import org.eclipse.hawkbit.ui.rollout.window.components.GroupsLegendLayout;
import org.eclipse.hawkbit.ui.rollout.window.components.RolloutFormLayout;
import org.eclipse.hawkbit.ui.rollout.window.components.SimpleGroupsLayout;
import org.eclipse.hawkbit.ui.rollout.window.components.VisualGroupDefinitionLayout;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Component;
import com.vaadin.ui.TabSheet;

/**
 * Builder for Rollout window components.
 */
public final class RolloutWindowLayoutComponentBuilder {

    private final RolloutWindowDependencies dependencies;

    private final DistributionSetStatelessDataProvider distributionSetDataProvider;
    private final TargetFilterQueryDataProvider targetFilterQueryDataProvider;

    /**
     * Constructor for RolloutWindowLayoutComponentBuilder
     *
     * @param rolloutWindowDependecies
     *            RolloutWindowDependencies
     */
    public RolloutWindowLayoutComponentBuilder(final RolloutWindowDependencies rolloutWindowDependecies) {
        this.dependencies = rolloutWindowDependecies;

        this.distributionSetDataProvider = new DistributionSetStatelessDataProvider(
                dependencies.getDistributionSetManagement(), new DistributionSetToProxyDistributionMapper());
        this.targetFilterQueryDataProvider = new TargetFilterQueryDataProvider(
                dependencies.getTargetFilterQueryManagement(), new TargetFilterQueryToProxyTargetFilterMapper());
    }

    /**
     * Create rollout form layout
     *
     * @return Form layout to create rollout
     */
    public RolloutFormLayout createRolloutFormLayout() {
        return new RolloutFormLayout(dependencies.getI18n(), distributionSetDataProvider,
                targetFilterQueryDataProvider);
    }

    /**
     * Create simple group layout
     *
     * @return Layout to create simple group
     */
    public SimpleGroupsLayout createSimpleGroupsLayout() {
        return new SimpleGroupsLayout(dependencies.getI18n(), dependencies.getQuotaManagement(),
                dependencies.getTenantConfigHelper(), dependencies.getUiProperties());
    }

    /**
     * Create advance group layout
     *
     * @return Layout to create advance group
     */
    public AdvancedGroupsLayout createAdvancedGroupsLayout() {
        return new AdvancedGroupsLayout(dependencies.getI18n(), dependencies.getEntityFactory(),
                dependencies.getRolloutManagement(), dependencies.getQuotaManagement(), targetFilterQueryDataProvider,
                dependencies.getTenantConfigHelper());
    }

    /**
     * Create group definition tabs
     *
     * @param simpleGroupDefinitionTab
     *            Simple group definition tab component
     * @param advancedGroupDefinitionTab
     *            Advance group definition tab component
     *
     * @return Group definition tab sheet
     */
    public TabSheet createGroupDefinitionTabs(final Component simpleGroupDefinitionTab,
            final Component advancedGroupDefinitionTab) {
        final TabSheet groupsDefinitionTabs = new TabSheet();
        groupsDefinitionTabs.setId(UIComponentIdProvider.ROLLOUT_GROUPS);
        groupsDefinitionTabs.setWidth(900, Unit.PIXELS);
        groupsDefinitionTabs.setHeight(300, Unit.PIXELS);
        groupsDefinitionTabs.setStyleName(SPUIStyleDefinitions.ROLLOUT_GROUPS);

        groupsDefinitionTabs
                .addTab(simpleGroupDefinitionTab, dependencies.getI18n().getMessage("caption.rollout.tabs.simple"))
                .setId(UIComponentIdProvider.ROLLOUT_SIMPLE_TAB);

        groupsDefinitionTabs
                .addTab(advancedGroupDefinitionTab, dependencies.getI18n().getMessage("caption.rollout.tabs.advanced"))
                .setId(UIComponentIdProvider.ROLLOUT_ADVANCED_TAB);

        return groupsDefinitionTabs;
    }

    /**
     * Create visual group definition layout
     * 
     * @return Group definition layout with Pie chat
     */
    public VisualGroupDefinitionLayout createVisualGroupDefinitionLayout() {
        return new VisualGroupDefinitionLayout(createGroupsPieChart(), createGroupsLegendLayout());
    }

    private static GroupsPieChart createGroupsPieChart() {
        final GroupsPieChart groupsPieChart = new GroupsPieChart();
        groupsPieChart.setWidth(260, Unit.PIXELS);
        groupsPieChart.setHeight(220, Unit.PIXELS);
        groupsPieChart.setStyleName(SPUIStyleDefinitions.ROLLOUT_GROUPS_CHART);

        return groupsPieChart;
    }

    private GroupsLegendLayout createGroupsLegendLayout() {
        return new GroupsLegendLayout(dependencies.getI18n());
    }

    /**
     * Create rollout approval layout
     *
     * @return Rollout approval layout
     */
    public ApprovalLayout createApprovalLayout() {
        return new ApprovalLayout(dependencies.getI18n());
    }
}
