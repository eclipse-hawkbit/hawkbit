/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.FilterParams;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.ui.common.UserDetailsFormatter;
import org.eclipse.hawkbit.ui.components.ProxyTarget;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

/**
 * Simple implementation of generics bean query which dynamically loads a batch
 * of beans.
 */
public class TargetBeanQuery extends AbstractBeanQuery<ProxyTarget> {

    private static final long serialVersionUID = -5645680058303167558L;

    private Sort sort = new Sort(SPUIDefinitions.TARGET_TABLE_CREATE_AT_SORT_ORDER, "id");
    private transient Collection<TargetUpdateStatus> status;
    private transient Boolean overdueState;
    private String[] targetTags;
    private Long distributionId;
    private String searchText;
    private Boolean noTagClicked;
    private transient TargetManagement targetManagement;
    private transient DeploymentManagement deploymentManagement;
    private transient VaadinMessageSource i18N;
    private Long pinnedDistId;
    private Long targetFilterQueryId;
    private ManagementUIState managementUIState;

    /**
     * Parametric Constructor.
     *
     * @param definition
     *            as Def
     * @param queryConfig
     *            as Config
     * @param sortIds
     *            as sort
     * @param sortStates
     *            as Sort status
     */
    public TargetBeanQuery(final QueryDefinition definition, final Map<String, Object> queryConfig,
            final Object[] sortIds, final boolean[] sortStates) {

        super(definition, queryConfig, sortIds, sortStates);

        if (HawkbitCommonUtil.isNotNullOrEmpty(queryConfig)) {
            status = (Collection<TargetUpdateStatus>) queryConfig.get(SPUIDefinitions.FILTER_BY_STATUS);
            overdueState = (Boolean) queryConfig.get(SPUIDefinitions.FILTER_BY_OVERDUE_STATE);
            targetTags = (String[]) queryConfig.get(SPUIDefinitions.FILTER_BY_TAG);
            noTagClicked = (Boolean) queryConfig.get(SPUIDefinitions.FILTER_BY_NO_TAG);
            distributionId = (Long) queryConfig.get(SPUIDefinitions.FILTER_BY_DISTRIBUTION);
            searchText = (String) queryConfig.get(SPUIDefinitions.FILTER_BY_TEXT);
            targetFilterQueryId = (Long) queryConfig.get(SPUIDefinitions.FILTER_BY_TARGET_FILTER_QUERY);
            if (!StringUtils.isEmpty(searchText)) {
                searchText = String.format("%%%s%%", searchText);
            }
            pinnedDistId = (Long) queryConfig.get(SPUIDefinitions.ORDER_BY_DISTRIBUTION);
        }

        if (sortStates != null && sortStates.length > 0) {

            sort = new Sort(sortStates[0] ? Direction.ASC : Direction.DESC, (String) sortIds[0]);

            for (int targetId = 1; targetId < sortIds.length; targetId++) {
                sort.and(new Sort(sortStates[targetId] ? Direction.ASC : Direction.DESC, (String) sortIds[targetId]));
            }
        }
    }

    @Override
    protected ProxyTarget constructBean() {
        return new ProxyTarget();
    }

    @Override
    protected List<ProxyTarget> loadBeans(final int startIndex, final int count) {
        Slice<Target> targetBeans;
        final List<ProxyTarget> proxyTargetBeans = new ArrayList<>();
        if (pinnedDistId != null) {
            targetBeans = getTargetManagement().findByFilterOrderByLinkedDistributionSet(
                    new OffsetBasedPageRequest(startIndex, SPUIDefinitions.PAGE_SIZE, sort), pinnedDistId,
                    new FilterParams(status, overdueState, searchText, distributionId, noTagClicked, targetTags));
        } else if (null != targetFilterQueryId) {
            targetBeans = getTargetManagement().findByTargetFilterQuery(
                    new PageRequest(startIndex / SPUIDefinitions.PAGE_SIZE, SPUIDefinitions.PAGE_SIZE, sort),
                    targetFilterQueryId);
        } else if (!isAnyFilterSelected()) {
            targetBeans = getTargetManagement()
                    .findAll(new PageRequest(startIndex / SPUIDefinitions.PAGE_SIZE, SPUIDefinitions.PAGE_SIZE, sort));
        } else {
            targetBeans = getTargetManagement().findByFilters(
                    new PageRequest(startIndex / SPUIDefinitions.PAGE_SIZE, SPUIDefinitions.PAGE_SIZE, sort),
                    new FilterParams(status, overdueState, searchText, distributionId, noTagClicked, targetTags));
        }
        for (final Target targ : targetBeans) {
            final ProxyTarget prxyTarget = new ProxyTarget();
            prxyTarget.setId(targ.getId());
            prxyTarget.setName(targ.getName());
            prxyTarget.setDescription(targ.getDescription());
            prxyTarget.setControllerId(targ.getControllerId());
            prxyTarget.setInstallationDate(targ.getInstallationDate());
            prxyTarget.setAddress(targ.getAddress());
            prxyTarget.setLastTargetQuery(targ.getLastTargetQuery());
            prxyTarget.setUpdateStatus(targ.getUpdateStatus());
            prxyTarget.setLastModifiedDate(SPDateTimeUtil.getFormattedDate(targ.getLastModifiedAt()));
            prxyTarget.setCreatedDate(SPDateTimeUtil.getFormattedDate(targ.getCreatedAt()));
            prxyTarget.setCreatedAt(targ.getCreatedAt());
            prxyTarget.setCreatedByUser(UserDetailsFormatter.loadAndFormatCreatedBy(targ));
            prxyTarget.setModifiedByUser(UserDetailsFormatter.loadAndFormatLastModifiedBy(targ));

            if (pinnedDistId == null) {
                prxyTarget.setInstalledDistributionSet(null);
                prxyTarget.setAssignedDistributionSet(null);
            } else {
                getDeploymentManagement().getAssignedDistributionSet(targ.getControllerId())
                        .ifPresent(prxyTarget::setAssignedDistributionSet);
                getDeploymentManagement().getInstalledDistributionSet(targ.getControllerId())
                        .ifPresent(prxyTarget::setInstalledDistributionSet);
            }

            prxyTarget.setUpdateStatus(targ.getUpdateStatus());
            prxyTarget.setLastTargetQuery(targ.getLastTargetQuery());
            prxyTarget.setPollStatusToolTip(HawkbitCommonUtil.getPollStatusToolTip(targ.getPollStatus(), getI18N()));
            proxyTargetBeans.add(prxyTarget);
        }
        return proxyTargetBeans;
    }

    private Boolean isTagSelected() {
        if (targetTags == null && !noTagClicked) {
            return false;
        }
        return true;
    }

    private boolean isOverdueFilterEnabled() {
        return Boolean.TRUE.equals(overdueState);
    }

    @Override
    protected void saveBeans(final List<ProxyTarget> addedTargets, final List<ProxyTarget> modifiedTargets,
            final List<ProxyTarget> removedTargets) {
        // CRUD operations on Target will be done through repository methods
    }

    @Override
    public int size() {
        final long totSize = getTargetManagement().count();
        long size;
        if (null != targetFilterQueryId) {
            size = getTargetManagement().countByTargetFilterQuery(targetFilterQueryId);
        } else if (!isAnyFilterSelected()) {
            size = totSize;
        } else {
            size = getTargetManagement().countByFilters(status, overdueState, searchText, distributionId, noTagClicked,
                    targetTags);
        }

        final ManagementUIState tmpManagementUIState = getManagementUIState();
        tmpManagementUIState.setTargetsCountAll(totSize);
        if (size > SPUIDefinitions.MAX_TABLE_ENTRIES) {
            tmpManagementUIState.setTargetsTruncated(size - SPUIDefinitions.MAX_TABLE_ENTRIES);
            size = SPUIDefinitions.MAX_TABLE_ENTRIES;
        } else {
            tmpManagementUIState.setTargetsTruncated(null);
        }

        return (int) size;
    }

    private boolean isAnyFilterSelected() {
        final boolean isFilterSelected = isTagSelected() || isOverdueFilterEnabled();
        return isFilterSelected || !CollectionUtils.isEmpty(status) || distributionId != null
                || !StringUtils.isEmpty(searchText);
    }

    private TargetManagement getTargetManagement() {
        if (targetManagement == null) {
            targetManagement = SpringContextHelper.getBean(TargetManagement.class);
        }
        return targetManagement;
    }

    private DeploymentManagement getDeploymentManagement() {
        if (deploymentManagement == null) {
            deploymentManagement = SpringContextHelper.getBean(DeploymentManagement.class);
        }
        return deploymentManagement;
    }

    private ManagementUIState getManagementUIState() {
        if (managementUIState == null) {
            managementUIState = SpringContextHelper.getBean(ManagementUIState.class);
        }
        return managementUIState;
    }

    private VaadinMessageSource getI18N() {
        if (i18N == null) {
            i18N = SpringContextHelper.getBean(VaadinMessageSource.class);
        }
        return i18N;
    }
}
