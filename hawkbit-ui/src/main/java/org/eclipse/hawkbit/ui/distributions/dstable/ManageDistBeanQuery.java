/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.dstable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter.DistributionSetFilterBuilder;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.ui.common.UserDetailsFormatter;
import org.eclipse.hawkbit.ui.components.ProxyDistribution;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

import com.google.common.base.Strings;

/**
 * Manage Distributions table bean query.
 *
 */
public class ManageDistBeanQuery extends AbstractBeanQuery<ProxyDistribution> {

    private static final long serialVersionUID = 5176481314404662215L;
    private Sort sort = new Sort(Direction.ASC, "createdAt");
    private String searchText = null;
    private transient DistributionSetManagement distributionSetManagement;
    private transient Page<DistributionSet> firstPageDistributionSets = null;

    private DistributionSetType distributionSetType = null;

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

        if (HawkbitCommonUtil.mapCheckStrKey(queryConfig)) {
            searchText = (String) queryConfig.get(SPUIDefinitions.FILTER_BY_TEXT);
            if (!Strings.isNullOrEmpty(searchText)) {
                searchText = String.format("%%%s%%", searchText);
            }
            if (null != queryConfig.get(SPUIDefinitions.FILTER_BY_DISTRIBUTION_SET_TYPE)) {
                distributionSetType = (DistributionSetType) queryConfig
                        .get(SPUIDefinitions.FILTER_BY_DISTRIBUTION_SET_TYPE);
            }
        }

        if (sortStates.length > 0) {
            // Initalize sort
            sort = new Sort(sortStates[0] ? Direction.ASC : Direction.DESC, (String) sortPropertyIds[0]);
            // Add sort
            for (int distId = 1; distId < sortPropertyIds.length; distId++) {
                sort.and(new Sort(sortStates[distId] ? Direction.ASC : Direction.DESC,
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
        } else if (Strings.isNullOrEmpty(searchText)) {
            // if no search filters available
            distBeans = getDistributionSetManagement().findDistributionSetsByDeletedAndOrCompleted(
                    new OffsetBasedPageRequest(startIndex, count, sort), false, null);
        } else {
            final DistributionSetFilter distributionSetFilter = new DistributionSetFilterBuilder().setIsDeleted(false)
                    .setSearchText(searchText).setSelectDSWithNoTag(Boolean.FALSE).setType(distributionSetType).build();
            distBeans = getDistributionSetManagement().findDistributionSetsByFilters(
                    new PageRequest(startIndex / count, count, sort), distributionSetFilter);
        }

        for (final DistributionSet distributionSet : distBeans) {
            final ProxyDistribution proxyDistribution = new ProxyDistribution();
            proxyDistribution.setName(distributionSet.getName());
            proxyDistribution.setDescription(distributionSet.getDescription());
            proxyDistribution.setDistId(distributionSet.getId());
            proxyDistribution.setId(distributionSet.getId());
            proxyDistribution.setVersion(distributionSet.getVersion());
            proxyDistribution.setCreatedDate(SPDateTimeUtil.getFormattedDate(distributionSet.getCreatedAt()));
            proxyDistribution.setLastModifiedDate(SPDateTimeUtil.getFormattedDate(distributionSet.getLastModifiedAt()));
            proxyDistribution.setCreatedByUser(UserDetailsFormatter.loadAndFormatCreatedBy(distributionSet));
            proxyDistribution.setModifiedByUser(UserDetailsFormatter.loadAndFormatLastModifiedBy(distributionSet));
            proxyDistribution.setIsComplete(distributionSet.isComplete());
            proxyDistributions.add(proxyDistribution);
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
        if (Strings.isNullOrEmpty(searchText) && null == distributionSetType) {
            // if no search filters available
            firstPageDistributionSets = getDistributionSetManagement().findDistributionSetsByDeletedAndOrCompleted(
                    new PageRequest(0, SPUIDefinitions.PAGE_SIZE, sort), false, null);
        } else {
            final DistributionSetFilter distributionSetFilter = new DistributionSetFilterBuilder().setIsDeleted(false)
                    .setSearchText(searchText).setSelectDSWithNoTag(Boolean.FALSE).setType(distributionSetType).build();
            firstPageDistributionSets = getDistributionSetManagement().findDistributionSetsByFilters(
                    new PageRequest(0, SPUIDefinitions.PAGE_SIZE, sort), distributionSetFilter);
        }
        final long size = firstPageDistributionSets.getTotalElements();

        if (size > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) size;
    }

    private DistributionSetManagement getDistributionSetManagement() {
        if (distributionSetManagement == null) {
            distributionSetManagement = SpringContextHelper.getBean(DistributionSetManagement.class);
        }
        return distributionSetManagement;
    }

    @SuppressWarnings("unchecked")
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        firstPageDistributionSets = (Page<DistributionSet>) in.readObject();
    }
}
