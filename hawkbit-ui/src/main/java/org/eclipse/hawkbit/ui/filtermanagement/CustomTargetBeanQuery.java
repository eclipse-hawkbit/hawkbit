/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.UserDetailsFormatter;
import org.eclipse.hawkbit.ui.components.ProxyTarget;
import org.eclipse.hawkbit.ui.filtermanagement.state.FilterManagementUIState;
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
 * Simple implementation of generics bean query which dynamically loads
 * {@link ProxyTarget} batch of beans.
 *
 */
public class CustomTargetBeanQuery extends AbstractBeanQuery<ProxyTarget> {

    private static final long serialVersionUID = 6490445732785388071L;
    private Sort sort = new Sort(Direction.DESC, "createdAt");
    private transient TargetManagement targetManagement;
    private FilterManagementUIState filterManagementUIState;
    private transient I18N i18N;
    private String filterQuery;

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
    public CustomTargetBeanQuery(final QueryDefinition definition, final Map<String, Object> queryConfig,
            final Object[] sortIds, final boolean[] sortStates) {
        super(definition, queryConfig, sortIds, sortStates);

        if (HawkbitCommonUtil.mapCheckStrKey(queryConfig)) {
            filterQuery = (String) queryConfig.get(SPUIDefinitions.FILTER_BY_QUERY);
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

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery#constructBean()
     */
    @Override
    protected ProxyTarget constructBean() {

        return new ProxyTarget();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery#loadBeans(int,
     * int)
     */
    @Override
    protected List<ProxyTarget> loadBeans(final int startIndex, final int count) {
        Slice<Target> targetBeans;
        final List<ProxyTarget> proxyTargetBeans = new ArrayList<>();
        if (!Strings.isNullOrEmpty(filterQuery)) {
            targetBeans = targetManagement.findTargetsAll(filterQuery,
                    new PageRequest(startIndex / SPUIDefinitions.PAGE_SIZE, SPUIDefinitions.PAGE_SIZE, sort));
        } else {
            targetBeans = targetManagement.findTargetsAll(
                    new PageRequest(startIndex / SPUIDefinitions.PAGE_SIZE, SPUIDefinitions.PAGE_SIZE, sort));
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

            prxyTarget.setUpdateStatus(targ.getTargetInfo().getUpdateStatus());
            prxyTarget.setLastTargetQuery(targ.getTargetInfo().getLastTargetQuery());
            prxyTarget.setTargetInfo(targ.getTargetInfo());
            prxyTarget.setPollStatusToolTip(
                    HawkbitCommonUtil.getPollStatusToolTip(prxyTarget.getTargetInfo().getPollStatus(), getI18N()));
            proxyTargetBeans.add(prxyTarget);
        }
        return proxyTargetBeans;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery#saveBeans(java
     * .util.List, java.util.List, java.util.List)
     */
    @Override
    protected void saveBeans(final List<ProxyTarget> arg0, final List<ProxyTarget> arg1, final List<ProxyTarget> arg2) {
        // CRUD operations on Target will be done through repository methods
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery#size()
     */
    @Override
    public int size() {
        long size = 0;
        if (!Strings.isNullOrEmpty(filterQuery)) {
            size = getTargetManagement().countTargetByTargetFilterQuery(filterQuery);
        }
        getFilterManagementUIState().setTargetsCountAll(size);
        if (size > SPUIDefinitions.MAX_TABLE_ENTRIES) {
            getFilterManagementUIState().setTargetsTruncated(size - SPUIDefinitions.MAX_TABLE_ENTRIES);
            size = SPUIDefinitions.MAX_TABLE_ENTRIES;
        } else {
            getFilterManagementUIState().setTargetsTruncated(null);
        }
        return (int) size;
    }

    private TargetManagement getTargetManagement() {
        if (targetManagement == null) {
            targetManagement = SpringContextHelper.getBean(TargetManagement.class);
        }
        return targetManagement;
    }

    private FilterManagementUIState getFilterManagementUIState() {
        if (filterManagementUIState == null) {
            filterManagementUIState = SpringContextHelper.getBean(FilterManagementUIState.class);
        }
        return filterManagementUIState;
    }

    private I18N getI18N() {
        if (i18N == null) {
            i18N = SpringContextHelper.getBean(I18N.class);
        }
        return i18N;
    }

}
