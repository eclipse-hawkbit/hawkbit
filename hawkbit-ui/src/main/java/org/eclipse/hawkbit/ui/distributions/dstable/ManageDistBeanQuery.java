/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.dstable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter.DistributionSetFilterBuilder;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.ui.components.ProxyDistribution;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.util.StringUtils;
import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

import com.vaadin.data.util.filter.SimpleStringFilter;

/**
 * Manage Distributions table bean query.
 *
 */
public class ManageDistBeanQuery extends AbstractBeanQuery<ProxyDistribution> {

    private static final long serialVersionUID = 1L;

    private Sort sort = new Sort(Direction.ASC, "id");
    private String searchText;
    private String filterString;
    private transient DistributionSetManagement distributionSetManagement;
    private transient Page<DistributionSet> firstPageDistributionSets;

    private transient DistributionSetType distributionSetType;
    private Boolean dsComplete;

    /**
     *
     * @param definition
     * @param queryConfig
     * @param sortPropertyIds
     * @param sortStates
     */
    public ManageDistBeanQuery(final QueryDefinition definition, final Map<String, Object> queryConfig,
            final Object[] sortPropertyIds, final boolean[] sortStates) {
        super(definition, queryConfig, sortPropertyIds, sortStates);

        init(definition, queryConfig, sortPropertyIds, sortStates);
    }

    private void init(final QueryDefinition definition, final Map<String, Object> queryConfig,
            final Object[] sortPropertyIds, final boolean[] sortStates) {
        populateDataFromQueryConfig(queryConfig);
        setFilterString(definition);
        setupSorting(sortPropertyIds, sortStates);
    }

    private void populateDataFromQueryConfig(final Map<String, Object> queryConfig) {
        if (HawkbitCommonUtil.isNotNullOrEmpty(queryConfig)) {
            searchText = (String) queryConfig.get(SPUIDefinitions.FILTER_BY_TEXT);
            if (!StringUtils.isEmpty(searchText)) {
                searchText = String.format("%%%s%%", searchText);
            }
            if (queryConfig.get(SPUIDefinitions.FILTER_BY_DISTRIBUTION_SET_TYPE) != null) {
                distributionSetType = (DistributionSetType) queryConfig
                        .get(SPUIDefinitions.FILTER_BY_DISTRIBUTION_SET_TYPE);
            }
            if (queryConfig.get(SPUIDefinitions.FILTER_BY_DS_COMPLETE) != null) {
                dsComplete = (Boolean) queryConfig.get(SPUIDefinitions.FILTER_BY_DS_COMPLETE);
            }
        }
    }

    private void setFilterString(final QueryDefinition definition) {
        // if search text is set, we do not want to apply the filter
        if (StringUtils.isEmpty(searchText)) {
            filterString = definition.getFilters().stream().filter(SimpleStringFilter.class::isInstance)
                    .map(SimpleStringFilter.class::cast).map(SimpleStringFilter::getFilterString).findAny()
                    .orElse(null);
        }
    }

    private void setupSorting(final Object[] sortPropertyIds, final boolean[] sortStates) {
        if (sortStates != null && sortStates.length > 0) {
            // Initialize sort
            sort = new Sort(sortStates[0] ? Direction.ASC : Direction.DESC, (String) sortPropertyIds[0]);
            // Add sort
            for (int distId = 1; distId < sortPropertyIds.length; distId++) {
                sort = sort.and(new Sort(sortStates[distId] ? Direction.ASC : Direction.DESC,
                        (String) sortPropertyIds[distId]));
            }
        }
    }

    @Override
    protected ProxyDistribution constructBean() {
        return new ProxyDistribution();
    }

    @Override
    protected List<ProxyDistribution> loadBeans(final int startIndex, final int count) {
        Page<DistributionSet> distBeans;
        final List<ProxyDistribution> proxyDistributions = new ArrayList<>();

        if (startIndex == 0 && firstPageDistributionSets != null) {
            distBeans = firstPageDistributionSets;
        } else {
            distBeans = findDistBeans(new OffsetBasedPageRequest(startIndex, count, sort));
        }

        for (final DistributionSet distributionSet : distBeans) {
            proxyDistributions.add(new ProxyDistribution(distributionSet));
        }
        return proxyDistributions;
    }

    @Override
    protected void saveBeans(final List<ProxyDistribution> arg0, final List<ProxyDistribution> arg1,
            final List<ProxyDistribution> arg2) {
        // Add,Delete and Update are performed through repository methods
    }

    @Override
    public int size() {
        firstPageDistributionSets = findDistBeans(new OffsetBasedPageRequest(0, SPUIDefinitions.PAGE_SIZE, sort));
        final long size = firstPageDistributionSets.getTotalElements();

        if (size > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) size;
    }

    private Page<DistributionSet> findDistBeans(final Pageable pageable) {
        if (StringUtils.isEmpty(filterString) && StringUtils.isEmpty(searchText) && distributionSetType == null) {
            return getDistributionSetManagement().findByCompleted(pageable, dsComplete);
        } else {
            final DistributionSetFilter distributionSetFilter = new DistributionSetFilterBuilder()
                    .setIsDeleted(Boolean.FALSE).setIsComplete(dsComplete).setSearchText(searchText)
                    .setFilterString(filterString).setSelectDSWithNoTag(Boolean.FALSE).setType(distributionSetType)
                    .build();

            return getDistributionSetManagement().findByDistributionSetFilter(pageable, distributionSetFilter);
        }
    }

    private DistributionSetManagement getDistributionSetManagement() {
        if (distributionSetManagement == null) {
            distributionSetManagement = SpringContextHelper.getBean(DistributionSetManagement.class);
        }
        return distributionSetManagement;
    }
}
