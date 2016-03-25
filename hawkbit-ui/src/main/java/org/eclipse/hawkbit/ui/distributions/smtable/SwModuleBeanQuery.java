/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.model.CustomSoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.springframework.data.domain.Slice;
import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

import com.google.common.base.Strings;

/**
 * Simple implementation of generics bean query which dynamically loads a batch
 * of beans.
 *
 */
public class SwModuleBeanQuery extends AbstractBeanQuery<ProxyBaseSwModuleItem> {
    private static final long serialVersionUID = 4362142538539335466L;
    private transient SoftwareManagement softwareManagementService;
    private SoftwareModuleType type;
    private String searchText = null;
    private Long orderByDistId = 0L;

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
    public SwModuleBeanQuery(final QueryDefinition definition, final Map<String, Object> queryConfig,
            final Object[] sortIds, final boolean[] sortStates) {
        super(definition, queryConfig, sortIds, sortStates);
        if (HawkbitCommonUtil.mapCheckStrKey(queryConfig)) {
            type = (SoftwareModuleType) queryConfig.get(SPUIDefinitions.BY_SOFTWARE_MODULE_TYPE);
            searchText = (String) queryConfig.get(SPUIDefinitions.FILTER_BY_TEXT);
            if (!Strings.isNullOrEmpty(searchText)) {
                searchText = String.format("%%%s%%", searchText);
            }
            orderByDistId = (Long) queryConfig.get(SPUIDefinitions.ORDER_BY_DISTRIBUTION);
            if (orderByDistId == null) {
                orderByDistId = 0L;
            }
        }
    }

    @Override
    protected ProxyBaseSwModuleItem constructBean() {
        return new ProxyBaseSwModuleItem();
    }

    @Override
    protected List<ProxyBaseSwModuleItem> loadBeans(final int startIndex, final int count) {
        final Slice<CustomSoftwareModule> swModuleBeans;
        final List<ProxyBaseSwModuleItem> proxyBeans = new ArrayList<>();

        swModuleBeans = getSoftwareManagement().findSoftwareModuleOrderByDistributionModuleNameAscModuleVersionAsc(
                new OffsetBasedPageRequest(startIndex, count), orderByDistId, searchText, type);

        for (final CustomSoftwareModule swModule : swModuleBeans) {
            proxyBeans.add(getProxyBean(swModule));
        }

        return proxyBeans;
    }

    private ProxyBaseSwModuleItem getProxyBean(final CustomSoftwareModule customSoftwareModule) {
        final SoftwareModule bean = customSoftwareModule.getSoftwareModule();
        final ProxyBaseSwModuleItem proxyItem = new ProxyBaseSwModuleItem();
        proxyItem.setSwId(bean.getId());
        final String swNameVersion = HawkbitCommonUtil.concatStrings(":", bean.getName(), bean.getVersion());
        proxyItem.setNameAndVersion(swNameVersion);
        proxyItem.setCreatedDate(SPDateTimeUtil.getFormattedDate(bean.getCreatedAt()));
        proxyItem.setLastModifiedDate(SPDateTimeUtil.getFormattedDate(bean.getLastModifiedAt()));
        proxyItem.setName(bean.getName());
        proxyItem.setVersion(bean.getVersion());
        proxyItem.setVendor(bean.getVendor());
        proxyItem.setDescription(bean.getDescription());
        proxyItem.setCreatedByUser(HawkbitCommonUtil.getIMUser(bean.getCreatedBy()));
        proxyItem.setModifiedByUser(HawkbitCommonUtil.getIMUser(bean.getLastModifiedBy()));
        proxyItem.setAssigned(customSoftwareModule.isAssigned());
        proxyItem.setColour(bean.getType().getColour());
        proxyItem.setTypeId(bean.getType().getId());
        return proxyItem;
    }

    @Override
    public int size() {
        long size;
        if (type == null && Strings.isNullOrEmpty(searchText)) {
            size = getSoftwareManagement().countSoftwareModulesAll();
        } else {
            size = getSoftwareManagement().countSoftwareModuleByFilters(searchText, type);
        }

        if (size > Integer.MAX_VALUE) {
            size = Integer.MAX_VALUE;
        }
        return (int) size;

    }

    @Override
    protected void saveBeans(final List<ProxyBaseSwModuleItem> addedBeans,
            final List<ProxyBaseSwModuleItem> modifiedBeans, final List<ProxyBaseSwModuleItem> removedBeans) {
        // save of the entity not required from this method
    }

    private SoftwareManagement getSoftwareManagement() {
        if (softwareManagementService == null) {
            softwareManagementService = SpringContextHelper.getBean(SoftwareManagement.class);
        }
        return softwareManagementService;
    }
}
