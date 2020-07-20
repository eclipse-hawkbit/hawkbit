/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.window.controllers;

import java.util.List;

import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.model.RepositoryModelConstants;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.ui.common.data.mappers.RolloutGroupToAdvancedDefinitionMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyAdvancedRolloutGroup;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRollout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRolloutWindow;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRolloutWindow.GroupDefinitionMode;
import org.eclipse.hawkbit.ui.rollout.window.RolloutWindowDependencies;
import org.eclipse.hawkbit.ui.rollout.window.components.AutoStartOptionGroupLayout.AutoStartOption;
import org.eclipse.hawkbit.ui.rollout.window.layouts.AddRolloutWindowLayout;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.CollectionUtils;

/**
 * Controller for populating data in Copy Rollout Window.
 */
public class CopyRolloutWindowController extends AddRolloutWindowController {

    private final TargetFilterQueryManagement targetFilterQueryManagement;
    private final RolloutGroupManagement rolloutGroupManagement;
    private final QuotaManagement quotaManagement;

    /**
     * Constructor for CopyRolloutWindowController
     *
     * @param dependencies
     *            RolloutWindowDependencies
     * @param layout
     *            AddRolloutWindowLayout
     */
    public CopyRolloutWindowController(final RolloutWindowDependencies dependencies,
            final AddRolloutWindowLayout layout) {
        super(dependencies, layout);

        this.targetFilterQueryManagement = dependencies.getTargetFilterQueryManagement();
        this.rolloutGroupManagement = dependencies.getRolloutGroupManagement();
        this.quotaManagement = dependencies.getQuotaManagement();
    }

    @Override
    protected ProxyRolloutWindow buildEntityFromProxy(final ProxyRollout proxyEntity) {
        final ProxyRolloutWindow proxyRolloutWindow = new ProxyRolloutWindow(proxyEntity);

        proxyRolloutWindow.setName(i18n.getMessage("textfield.rollout.copied.name", proxyRolloutWindow.getName()));

        setTargetFilterId(proxyRolloutWindow);

        if (proxyRolloutWindow.getForcedTime() == null
                || RepositoryModelConstants.NO_FORCE_TIME.equals(proxyRolloutWindow.getForcedTime())) {
            proxyRolloutWindow.setForcedTime(SPDateTimeUtil.twoWeeksFromNowEpochMilli());
        }

        proxyRolloutWindow.setAutoStartOption(proxyRolloutWindow.getOptionByStartAt());
        if (AutoStartOption.SCHEDULED != proxyRolloutWindow.getAutoStartOption()) {
            proxyRolloutWindow.setStartAt(SPDateTimeUtil.halfAnHourFromNowEpochMilli());
        }

        setAdvancedGroups(proxyRolloutWindow);

        if (CollectionUtils.isEmpty(proxyRolloutWindow.getAdvancedRolloutGroupDefinitions())) {
            setDefaultThresholds(proxyRolloutWindow);
        } else {
            setThresholdsOfFirstGroup(proxyRolloutWindow);
        }

        return proxyRolloutWindow;
    }

    private void setTargetFilterId(final ProxyRolloutWindow proxyRolloutWindow) {
        final Page<TargetFilterQuery> filterQueries = targetFilterQueryManagement.findByQuery(PageRequest.of(0, 1),
                proxyRolloutWindow.getTargetFilterQuery());
        if (filterQueries.getTotalElements() > 0) {
            proxyRolloutWindow.setTargetFilterId(filterQueries.getContent().get(0).getId());
        }
    }

    private void setAdvancedGroups(final ProxyRolloutWindow proxyRolloutWindow) {
        proxyRolloutWindow.setGroupDefinitionMode(GroupDefinitionMode.ADVANCED);
        final RolloutGroupToAdvancedDefinitionMapper groupsMapper = new RolloutGroupToAdvancedDefinitionMapper(
                targetFilterQueryManagement);
        final List<ProxyAdvancedRolloutGroup> advancedGroupDefinitions = groupsMapper.loadRolloutGroupssFromBackend(
                proxyRolloutWindow.getId(), rolloutGroupManagement, quotaManagement.getMaxRolloutGroupsPerRollout());
        proxyRolloutWindow.setAdvancedRolloutGroupDefinitions(advancedGroupDefinitions);
    }

    private static void setThresholdsOfFirstGroup(final ProxyRolloutWindow proxyRolloutWindow) {
        final ProxyAdvancedRolloutGroup firstAdvancedRolloutGroup = proxyRolloutWindow
                .getAdvancedRolloutGroupDefinitions().get(0);
        proxyRolloutWindow.setTriggerThresholdPercentage(firstAdvancedRolloutGroup.getTriggerThresholdPercentage());
        proxyRolloutWindow.setErrorThresholdPercentage(firstAdvancedRolloutGroup.getErrorThresholdPercentage());
    }

    @Override
    protected void adaptLayout(final ProxyRollout proxyEntity) {
        layout.selectAdvancedGroupsTab();
    }
}
