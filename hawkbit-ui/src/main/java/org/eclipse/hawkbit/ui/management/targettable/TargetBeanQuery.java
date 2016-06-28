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

import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.ui.common.UserDetailsFormatter;
import org.eclipse.hawkbit.ui.components.ProxyTarget;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

import com.google.common.base.Strings;

/**
 * Simple implementation of generics bean query which dynamically loads a batch
 * of beans.
 *
 */
public class TargetBeanQuery extends AbstractBeanQuery<ProxyTarget> {
    private static final long serialVersionUID = -5645680058303167558L;
    private Sort sort = new Sort(SPUIDefinitions.TARGET_TABLE_CREATE_AT_SORT_ORDER, "createdAt");
    private Collection<TargetUpdateStatus> status = null;
    private String[] targetTags = null;
    private Long distributionId = null;
    private String searchText = null;
    private Boolean noTagClicked = Boolean.FALSE;
    private transient TargetManagement targetManagement;
    private transient I18N i18N;
    private Long pinnedDistId = null;
    private TargetFilterQuery targetFilterQuery;
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

        if (HawkbitCommonUtil.mapCheckStrKey(queryConfig)) {
            status = (Collection<TargetUpdateStatus>) queryConfig.get(SPUIDefinitions.FILTER_BY_STATUS);
            targetTags = (String[]) queryConfig.get(SPUIDefinitions.FILTER_BY_TAG);
            noTagClicked = (Boolean) queryConfig.get(SPUIDefinitions.FILTER_BY_NO_TAG);
            distributionId = (Long) queryConfig.get(SPUIDefinitions.FILTER_BY_DISTRIBUTION);
            searchText = (String) queryConfig.get(SPUIDefinitions.FILTER_BY_TEXT);
            targetFilterQuery = (TargetFilterQuery) queryConfig.get(SPUIDefinitions.FILTER_BY_TARGET_FILTER_QUERY);
            if (!Strings.isNullOrEmpty(searchText)) {
                searchText = String.format("%%%s%%", searchText);
            }
            pinnedDistId = (Long) queryConfig.get(SPUIDefinitions.ORDER_BY_DISTRIBUTION);
        }

        if (HawkbitCommonUtil.checkBolArray(sortStates)) {
            // Initalize Sor
            sort = new Sort(sortStates[0] ? Direction.ASC : Direction.DESC, (String) sortIds[0]);
            // Add sort.
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
            targetBeans = getTargetManagement().findTargetsAllOrderByLinkedDistributionSet(
                    new OffsetBasedPageRequest(startIndex, SPUIDefinitions.PAGE_SIZE, sort), pinnedDistId,
                    distributionId, status, searchText, noTagClicked, targetTags);
        } else if (null != targetFilterQuery) {
            targetBeans = getTargetManagement().findTargetsAll(targetFilterQuery,
                    new PageRequest(startIndex / SPUIDefinitions.PAGE_SIZE, SPUIDefinitions.PAGE_SIZE, sort));
        } else if (!anyFilterSelected()) {
            targetBeans = getTargetManagement().findTargetsAll(
                    new PageRequest(startIndex / SPUIDefinitions.PAGE_SIZE, SPUIDefinitions.PAGE_SIZE, sort));
        } else {
            targetBeans = getTargetManagement().findTargetByFilters(
                    new PageRequest(startIndex / SPUIDefinitions.PAGE_SIZE, SPUIDefinitions.PAGE_SIZE, sort), status,
                    searchText, distributionId, noTagClicked, targetTags);
        }
        for (final Target targ : targetBeans) {
            final ProxyTarget prxyTarget = new ProxyTarget();
            prxyTarget.setTargetIdName(targ.getTargetIdName());
            prxyTarget.setName(targ.getName());
            prxyTarget.setDescription(targ.getDescription());
            prxyTarget.setControllerId(targ.getControllerId());
            prxyTarget.setInstallationDate(targ.getTargetInfo().getInstallationDate());
            prxyTarget.setAddress(targ.getTargetInfo().getAddress());
            prxyTarget.setLastTargetQuery(targ.getTargetInfo().getLastTargetQuery());
            prxyTarget.setUpdateStatus(targ.getTargetInfo().getUpdateStatus());
            prxyTarget.setLastModifiedDate(SPDateTimeUtil.getFormattedDate(targ.getLastModifiedAt()));
            prxyTarget.setCreatedDate(SPDateTimeUtil.getFormattedDate(targ.getCreatedAt()));
            prxyTarget.setCreatedAt(targ.getCreatedAt());
            prxyTarget.setCreatedByUser(UserDetailsFormatter.loadAndFormatCreatedBy(targ));
            prxyTarget.setModifiedByUser(UserDetailsFormatter.loadAndFormatLastModifiedBy(targ));

            if (pinnedDistId == null) {
                prxyTarget.setInstalledDistributionSet(null);
                prxyTarget.setAssignedDistributionSet(null);
            } else {
                final Target target = getTargetManagement().findTargetByControllerIDWithDetails(targ.getControllerId());
                final DistributionSet installedDistributionSet = target.getTargetInfo().getInstalledDistributionSet();
                prxyTarget.setInstalledDistributionSet(installedDistributionSet);
                final DistributionSet assignedDistributionSet = target.getAssignedDistributionSet();
                prxyTarget.setAssignedDistributionSet(assignedDistributionSet);
            }

            prxyTarget.setUpdateStatus(targ.getTargetInfo().getUpdateStatus());
            prxyTarget.setLastTargetQuery(targ.getTargetInfo().getLastTargetQuery());
            prxyTarget.setTargetInfo(targ.getTargetInfo());
            prxyTarget.setPollStatusToolTip(
                    HawkbitCommonUtil.getPollStatusToolTip(prxyTarget.getTargetInfo().getPollStatus(), getI18N()));
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

    @Override
    protected void saveBeans(final List<ProxyTarget> addedTargets, final List<ProxyTarget> modifiedTargets,
            final List<ProxyTarget> removedTargets) {
        // CRUD operations on Target will be done through repository methods
    }

    private Boolean anyFilterSelected() {
        if (status == null && distributionId == null && Strings.isNullOrEmpty(searchText) && !isTagSelected()) {
            return false;
        }
        return true;
    }

    @Override
    public int size() {
        final long totSize = getTargetManagement().countTargetsAll();
        long size;
        if (null != targetFilterQuery) {
            size = getTargetManagement().countTargetByTargetFilterQuery(targetFilterQuery);
        } else if (!anyFilterSelected()) {
            size = totSize;
        } else {
            size = getTargetManagement().countTargetByFilters(status, searchText, distributionId, noTagClicked,
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

    private TargetManagement getTargetManagement() {
        if (targetManagement == null) {
            targetManagement = SpringContextHelper.getBean(TargetManagement.class);
        }
        return targetManagement;
    }

    private ManagementUIState getManagementUIState() {
        if (managementUIState == null) {
            managementUIState = SpringContextHelper.getBean(ManagementUIState.class);
        }
        return managementUIState;
    }

    private I18N getI18N() {
        if (i18N == null) {
            i18N = SpringContextHelper.getBean(I18N.class);
        }
        return i18N;
    }

}
