/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import static org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil.isNotNullOrEmpty;
import static org.eclipse.hawkbit.ui.utils.SPUIDefinitions.FILTER_BY_QUERY;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.UserDetailsFormatter;
import org.eclipse.hawkbit.ui.components.ProxyTarget;
import org.eclipse.hawkbit.ui.filtermanagement.state.FilterManagementUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.util.StringUtils;
import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

/**
 * Simple implementation of generics bean query which dynamically loads
 * {@link ProxyTarget} batch of beans.
 *
 */
public class CustomTargetBeanQuery extends AbstractBeanQuery<ProxyTarget> {

    private static final long serialVersionUID = 6490445732785388071L;
    private Sort sort = new Sort(Direction.ASC, "id");
    private transient TargetManagement targetManagement;
    private FilterManagementUIState filterManagementUIState;
    private transient VaadinMessageSource i18N;
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

        if (isNotNullOrEmpty(queryConfig)) {
            filterQuery = (String) queryConfig.get(FILTER_BY_QUERY);
        }

        if (sortStates != null && sortStates.length > 0) {

            sort = new Sort(sortStates[0] ? ASC : DESC, (String) sortIds[0]);

            for (int targetId = 1; targetId < sortIds.length; targetId++) {
                sort.and(new Sort(sortStates[targetId] ? ASC : DESC, (String) sortIds[targetId]));
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
        if (!StringUtils.isEmpty(filterQuery)) {
            targetBeans = targetManagement.findByRsql(new PageRequest(startIndex / SPUIDefinitions.PAGE_SIZE, SPUIDefinitions.PAGE_SIZE, sort),
                    filterQuery);
        } else {
            targetBeans = targetManagement.findAll(
                    new PageRequest(startIndex / SPUIDefinitions.PAGE_SIZE, SPUIDefinitions.PAGE_SIZE, sort));
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

            prxyTarget.setUpdateStatus(targ.getUpdateStatus());
            prxyTarget.setLastTargetQuery(targ.getLastTargetQuery());
            prxyTarget.setPollStatusToolTip(HawkbitCommonUtil.getPollStatusToolTip(targ.getPollStatus(), getI18N()));
            proxyTargetBeans.add(prxyTarget);
        }
        return proxyTargetBeans;
    }

    @Override
    protected void saveBeans(final List<ProxyTarget> arg0, final List<ProxyTarget> arg1, final List<ProxyTarget> arg2) {
        // CRUD operations on Target will be done through repository methods
    }

    @Override
    public int size() {
        long size = 0;
        if (!StringUtils.isEmpty(filterQuery)) {
            size = getTargetManagement().countByRsql(filterQuery);
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

    private VaadinMessageSource getI18N() {
        if (i18N == null) {
            i18N = SpringContextHelper.getBean(VaadinMessageSource.class);
        }
        return i18N;
    }
}
