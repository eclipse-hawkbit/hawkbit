/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.common.UserDetailsFormatter;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.util.StringUtils;
import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

/**
 * Simple implementation of generics bean query which dynamically loads a batch
 * of beans.
 *
 */
public class BaseSwModuleBeanQuery extends AbstractBeanQuery<ProxyBaseSoftwareModuleItem> {
    private static final long serialVersionUID = 4362142538539335466L;
    private transient SoftwareModuleManagement softwareManagementService;
    private Long type;
    private String searchText;
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
        if (HawkbitCommonUtil.isNotNullOrEmpty(queryConfig)) {
            type = Optional.ofNullable((SoftwareModuleType) queryConfig.get(SPUIDefinitions.BY_SOFTWARE_MODULE_TYPE))
                    .map(SoftwareModuleType::getId).orElse(null);
            searchText = (String) queryConfig.get(SPUIDefinitions.FILTER_BY_TEXT);
            if (!StringUtils.isEmpty(searchText)) {
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

        if (type == null && StringUtils.isEmpty(searchText)) {
            swModuleBeans = getSoftwareManagementService().findAll(new OffsetBasedPageRequest(startIndex, count, sort));
        } else {
            swModuleBeans = getSoftwareManagementService()
                    .findByTextAndType(new OffsetBasedPageRequest(startIndex, count, sort), searchText, type);
        }

        return swModuleBeans.getContent().stream().map(this::getProxyBean).collect(Collectors.toList());
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
        proxy.setCreatedByUser(UserDetailsFormatter.loadAndFormatCreatedBy(bean));
        proxy.setModifiedByUser(UserDetailsFormatter.loadAndFormatLastModifiedBy(bean));
        return proxy;
    }

    @Override
    public int size() {
        long size;
        if (type == null && StringUtils.isEmpty(searchText)) {
            size = getSoftwareManagementService().count();
        } else {
            size = getSoftwareManagementService().countByTextAndType(searchText, type);
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

    private SoftwareModuleManagement getSoftwareManagementService() {
        if (softwareManagementService == null) {
            softwareManagementService = SpringContextHelper.getBean(SoftwareModuleManagement.class);
        }
        return softwareManagementService;
    }
}
