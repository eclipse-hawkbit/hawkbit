/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
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
 *
 *
 *
 *
 */
public class BaseSwModuleBeanQuery extends AbstractBeanQuery<ProxyBaseSoftwareModuleItem> {
    private static final long serialVersionUID = 4362142538539335466L;
    private transient SoftwareManagement softwareManagementService;
    private SoftwareModuleType type;
    private String searchText = null;
    private final Sort sort = new Sort(Direction.ASC, "name", "version");

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
    public BaseSwModuleBeanQuery(final QueryDefinition definition, final Map<String, Object> queryConfig,
            final Object[] sortIds, final boolean[] sortStates) {
        super(definition, queryConfig, sortIds, sortStates);
        if (HawkbitCommonUtil.mapCheckStrKey(queryConfig)) {
            type = (SoftwareModuleType) queryConfig.get(SPUIDefinitions.BY_SOFTWARE_MODULE_TYPE);
            searchText = (String) queryConfig.get(SPUIDefinitions.FILTER_BY_TEXT);
            if (!Strings.isNullOrEmpty(searchText)) {
                searchText = String.format("%%%s%%", searchText);
            }
        }
    }

    @Override
    protected ProxyBaseSoftwareModuleItem constructBean() {
        return new ProxyBaseSoftwareModuleItem();
    }

    @Override
    protected List<ProxyBaseSoftwareModuleItem> loadBeans(final int startIndex, final int count) {
        final Slice<SoftwareModule> swModuleBeans;
        final List<ProxyBaseSoftwareModuleItem> proxyBeans = new ArrayList<ProxyBaseSoftwareModuleItem>();

        if (type == null && Strings.isNullOrEmpty(searchText)) {
            swModuleBeans = getSoftwareManagementService()
                    .findSoftwareModulesAll(new OffsetBasedPageRequest(startIndex, count, sort));

        } else {
            swModuleBeans = getSoftwareManagementService()
                    .findSoftwareModuleByFilters(new OffsetBasedPageRequest(startIndex, count, sort), searchText, type);
        }

        for (final SoftwareModule swModule : swModuleBeans) {
            proxyBeans.add(getProxyBean(swModule));
        }

        return proxyBeans;
    }

    private ProxyBaseSoftwareModuleItem getProxyBean(final SoftwareModule bean) {
        final ProxyBaseSoftwareModuleItem proxy = new ProxyBaseSoftwareModuleItem();
        proxy.setSwId(bean.getId());
        final String swNameVersion = HawkbitCommonUtil.concatStrings(":", bean.getName(), bean.getVersion());
        proxy.setNameAndVersion(swNameVersion);
        proxy.setCreatedDate(SPDateTimeUtil.getFormattedDate(bean.getCreatedAt()));
        proxy.setLastModifiedDate(SPDateTimeUtil.getFormattedDate(bean.getLastModifiedAt()));
        proxy.setName(bean.getName());
        proxy.setVersion(bean.getVersion());
        proxy.setVendor(bean.getVendor());
        proxy.setDescription(bean.getDescription());
        proxy.setCreatedByUser(HawkbitCommonUtil.getIMUser(bean.getCreatedBy()));
        proxy.setModifiedByUser(HawkbitCommonUtil.getIMUser(bean.getLastModifiedBy()));
        return proxy;
    }

    @Override
    public int size() {
        long size;
        if (type == null && Strings.isNullOrEmpty(searchText)) {
            size = getSoftwareManagementService().countSoftwareModulesAll();
        } else {
            size = getSoftwareManagementService().countSoftwareModuleByFilters(searchText, type);
        }

        if (size > Integer.MAX_VALUE) {
            size = Integer.MAX_VALUE;
        }
        return (int) size;

    }

    @Override
    protected void saveBeans(final List<ProxyBaseSoftwareModuleItem> addedBeans,
            final List<ProxyBaseSoftwareModuleItem> modifiedBeans,
            final List<ProxyBaseSoftwareModuleItem> removedBeans) {
        // save of the entity not required from this method
    }

    private SoftwareManagement getSoftwareManagementService() {
        if (softwareManagementService == null) {
            softwareManagementService = SpringContextHelper.getBean(SoftwareManagement.class);
        }
        return softwareManagementService;
    }
}
