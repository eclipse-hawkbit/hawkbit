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
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetFilterQueryInfo;
import org.eclipse.hawkbit.ui.rollout.window.RolloutWindowDependencies;
import org.eclipse.hawkbit.ui.rollout.window.components.AutoStartOptionGroupLayout.AutoStartOption;
import org.eclipse.hawkbit.ui.rollout.window.layouts.AddRolloutWindowLayout;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.util.CollectionUtils;

/**
 * Controller for populating data in Copy Rollout Window.
 */
public class CopyRolloutWindowController extends AddRolloutWindowController {

    private final TargetFilterQueryManagement targetFilterQueryManagement;
    private final RolloutGroupManagement rolloutGroupManagement;
    private final QuotaManagement quotaManagement;
    private final TenantConfigHelper tenantConfigHelper;
    
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
        this.tenantConfigHelper = dependencies.getTenantConfigHelper();
    }

    @Override
    protected ProxyRolloutWindow buildEntityFromProxy(final ProxyRollout proxyEntity) {
        final ProxyRolloutWindow proxyRolloutWindow = new ProxyRolloutWindow(proxyEntity);

        proxyRolloutWindow.setName(getI18n().getMessage("textfield.rollout.copied.name", proxyRolloutWindow.getName()));

        removeDistributionSetIfInvalid(proxyRolloutWindow);
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

    private void removeDistributionSetIfInvalid(final ProxyRolloutWindow proxyRolloutWindow) {
        final boolean dsIsValid = proxyRolloutWindow.getRolloutForm().getDistributionSetInfo().isValid();
        if (!dsIsValid) {
            proxyRolloutWindow.getRolloutForm().setDistributionSetInfo(null);
        }

    }

    private void setTargetFilterId(final ProxyRolloutWindow proxyRolloutWindow) {
        final Slice<TargetFilterQuery> filterQueries = targetFilterQueryManagement.findByQuery(PageRequest.of(0, 1),
                proxyRolloutWindow.getTargetFilterQuery());
        if (filterQueries.getNumberOfElements() > 0) {
            final TargetFilterQuery tfq = filterQueries.getContent().get(0);
            proxyRolloutWindow
                    .setTargetFilterInfo(new ProxyTargetFilterQueryInfo(tfq.getId(), tfq.getName(), tfq.getQuery()));
        }
    }

    private void setAdvancedGroups(final ProxyRolloutWindow proxyRolloutWindow) {
        proxyRolloutWindow.setGroupDefinitionMode(GroupDefinitionMode.ADVANCED);
        final RolloutGroupToAdvancedDefinitionMapper groupsMapper = new RolloutGroupToAdvancedDefinitionMapper(
                targetFilterQueryManagement);
        final List<ProxyAdvancedRolloutGroup> advancedGroupDefinitions = groupsMapper.loadRolloutGroupsFromBackend(
                proxyRolloutWindow.getId(), rolloutGroupManagement, quotaManagement.getMaxRolloutGroupsPerRollout());
        if (!tenantConfigHelper.isConfirmationFlowEnabled()) {
            // do not require confirmation, since feature is not active and UI elements are not visible
            advancedGroupDefinitions.forEach(def -> def.setConfirmationRequired(false));
        }
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
