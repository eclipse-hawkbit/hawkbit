/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.model.AssignedSoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.common.UserDetailsFormatter;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.springframework.util.StringUtils;
import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

/**
 * Simple implementation of generics bean query which dynamically loads a batch
 * of beans.
 *
 */
public class SwModuleBeanQuery extends AbstractBeanQuery<ProxyBaseSwModuleItem> {
    private static final long serialVersionUID = 4362142538539335466L;
    private transient SoftwareModuleManagement softwareManagementService;
    private final Long type;
    private final String searchText;
    private final Long orderByDistId;

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
        if (HawkbitCommonUtil.isNotNullOrEmpty(queryConfig)) {
            type = Optional.ofNullable((SoftwareModuleType) queryConfig.get(SPUIDefinitions.BY_SOFTWARE_MODULE_TYPE))
                    .map(SoftwareModuleType::getId).orElse(null);
            final String text = (String) queryConfig.get(SPUIDefinitions.FILTER_BY_TEXT);
            if (!StringUtils.isEmpty(text)) {
                searchText = String.format("%%%s%%", text);
            } else {
                searchText = null;
            }
            orderByDistId = Optional.ofNullable((Long) queryConfig.get(SPUIDefinitions.ORDER_BY_DISTRIBUTION))
                    .orElse(0L);
            return;
        }

        orderByDistId = 0L;
        type = null;
        searchText = null;
    }

    @Override
    protected ProxyBaseSwModuleItem constructBean() {
        return new ProxyBaseSwModuleItem();
    }

    @Override
    protected List<ProxyBaseSwModuleItem> loadBeans(final int startIndex, final int count) {
        return getSoftwareModuleManagement()
                .findAllOrderBySetAssignmentAndModuleNameAscModuleVersionAsc(
                        new OffsetBasedPageRequest(startIndex, count), orderByDistId, searchText, type)
                .getContent().stream().map(SwModuleBeanQuery::getProxyBean).collect(Collectors.toList());
    }

    private static ProxyBaseSwModuleItem getProxyBean(final AssignedSoftwareModule customSoftwareModule) {
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
        proxyItem.setCreatedByUser(UserDetailsFormatter.loadAndFormatCreatedBy(bean));
        proxyItem.setModifiedByUser(UserDetailsFormatter.loadAndFormatLastModifiedBy(bean));
        proxyItem.setAssigned(customSoftwareModule.isAssigned());
        proxyItem.setColour(bean.getType().getColour());
        proxyItem.setTypeId(bean.getType().getId());
        return proxyItem;
    }

    @Override
    public int size() {
        long size;
        if (type == null && StringUtils.isEmpty(searchText)) {
            size = getSoftwareModuleManagement().count();
        } else {
            size = getSoftwareModuleManagement().countByTextAndType(searchText, type);
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

    private SoftwareModuleManagement getSoftwareModuleManagement() {
        if (softwareManagementService == null) {
            softwareManagementService = SpringContextHelper.getBean(SoftwareModuleManagement.class);
        }
        return softwareManagementService;
    }
}
