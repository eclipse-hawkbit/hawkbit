/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter.DistributionSetFilterBuilder;
import org.eclipse.hawkbit.ui.common.UserDetailsFormatter;
import org.eclipse.hawkbit.ui.common.entity.TargetIdName;
import org.eclipse.hawkbit.ui.components.ProxyDistribution;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
public class DistributionBeanQuery extends AbstractBeanQuery<ProxyDistribution> {

    private static final long serialVersionUID = 5862679853949173536L;
    private Sort sort = new Sort(Direction.ASC, "id");
    private Collection<String> distributionTags;
    private String searchText;
    private TargetIdName pinnedTarget;
    private transient DistributionSetManagement distributionSetManagement;
    private transient Page<DistributionSet> firstPageDistributionSets;
    private Boolean noTagClicked = Boolean.FALSE;

    /**
     * Bean query for retrieving beans/objects of type.
     *
     * @param definition
     *            query definition
     * @param queryConfig
     *            as queryConfig
     * @param sortPropertyIds
     *            property id's for sorting
     * @param sortStates
     *            sort states
     */
    public DistributionBeanQuery(final QueryDefinition definition, final Map<String, Object> queryConfig,
            final Object[] sortPropertyIds, final boolean[] sortStates) {
        super(definition, queryConfig, sortPropertyIds, sortStates);

        if (HawkbitCommonUtil.isNotNullOrEmpty(queryConfig)) {
            distributionTags = (Collection<String>) queryConfig.get(SPUIDefinitions.FILTER_BY_TAG);
            searchText = (String) queryConfig.get(SPUIDefinitions.FILTER_BY_TEXT);
            noTagClicked = (Boolean) queryConfig.get(SPUIDefinitions.FILTER_BY_NO_TAG);
            pinnedTarget = (TargetIdName) queryConfig.get(SPUIDefinitions.ORDER_BY_PINNED_TARGET);
            if (!StringUtils.isEmpty(searchText)) {
                searchText = String.format("%%%s%%", searchText);
            }
        }

        if (sortStates != null && sortStates.length > 0) {
            // Initalize sort
            sort = new Sort(sortStates[0] ? Direction.ASC : Direction.DESC, (String) sortPropertyIds[0]);
            // Add sort
            for (int distId = 1; distId < sortPropertyIds.length; distId++) {
                sort.and(new Sort(sortStates[distId] ? Direction.ASC : Direction.DESC,
                        (String) sortPropertyIds[distId]));
            }
        }
    }

    /**
     * Load all the Distribution set.
     *
     * @param startIndex
     *            as page start
     * @param count
     *            as total data
     */
    @Override
    protected List<ProxyDistribution> loadBeans(final int startIndex, final int count) {
        Page<DistributionSet> distBeans;
        final List<ProxyDistribution> proxyDistributions = new ArrayList<>();
        if (startIndex == 0 && firstPageDistributionSets != null) {
            distBeans = firstPageDistributionSets;
        } else if (pinnedTarget != null) {
            final DistributionSetFilterBuilder distributionSetFilterBuilder = new DistributionSetFilterBuilder()
                    .setIsDeleted(false).setIsComplete(true).setSearchText(searchText)
                    .setSelectDSWithNoTag(noTagClicked).setTagNames(distributionTags);

            distBeans = getDistributionSetManagement().findByFilterAndAssignedInstalledDsOrderedByLinkTarget(
                    new OffsetBasedPageRequest(startIndex, count, sort), distributionSetFilterBuilder,
                    pinnedTarget.getControllerId());
        } else if (distributionTags.isEmpty() && StringUtils.isEmpty(searchText) && !noTagClicked) {
            // if no search filters available
            distBeans = getDistributionSetManagement()
                    .findByCompleted(new OffsetBasedPageRequest(startIndex, count, sort), true);
        } else {
            final DistributionSetFilter distributionSetFilter = new DistributionSetFilterBuilder().setIsDeleted(false)
                    .setIsComplete(true).setSearchText(searchText).setSelectDSWithNoTag(noTagClicked)
                    .setTagNames(distributionTags).build();
            distBeans = getDistributionSetManagement().findByDistributionSetFilter(
                    new OffsetBasedPageRequest(startIndex, count, sort), distributionSetFilter);
        }

        for (final DistributionSet distributionSet : distBeans) {
            final ProxyDistribution proxyDistribution = new ProxyDistribution();
            proxyDistribution.setName(distributionSet.getName());
            proxyDistribution.setDescription(distributionSet.getDescription());
            proxyDistribution.setId(distributionSet.getId());
            proxyDistribution.setDistId(distributionSet.getId());
            proxyDistribution.setVersion(distributionSet.getVersion());
            proxyDistribution.setCreatedDate(SPDateTimeUtil.getFormattedDate(distributionSet.getCreatedAt()));
            proxyDistribution.setLastModifiedDate(SPDateTimeUtil.getFormattedDate(distributionSet.getLastModifiedAt()));
            proxyDistribution.setCreatedByUser(UserDetailsFormatter.loadAndFormatCreatedBy(distributionSet));
            proxyDistribution.setModifiedByUser(UserDetailsFormatter.loadAndFormatLastModifiedBy(distributionSet));
            proxyDistribution.setNameVersion(
                    HawkbitCommonUtil.getFormattedNameVersion(distributionSet.getName(), distributionSet.getVersion()));
            proxyDistributions.add(proxyDistribution);
        }
        return proxyDistributions;
    }

    @Override
    public int size() {
        if (pinnedTarget != null) {

            final DistributionSetFilterBuilder distributionSetFilterBuilder = new DistributionSetFilterBuilder()
                    .setIsDeleted(false).setIsComplete(true).setSearchText(searchText)
                    .setSelectDSWithNoTag(noTagClicked).setTagNames(distributionTags);

            firstPageDistributionSets = getDistributionSetManagement()
                    .findByFilterAndAssignedInstalledDsOrderedByLinkTarget(
                            new PageRequest(0, SPUIDefinitions.PAGE_SIZE, sort), distributionSetFilterBuilder,
                            pinnedTarget.getControllerId());
        } else if (distributionTags.isEmpty() && StringUtils.isEmpty(searchText) && !noTagClicked) {
            // if no search filters available
            firstPageDistributionSets = getDistributionSetManagement()
                    .findByCompleted(new PageRequest(0, SPUIDefinitions.PAGE_SIZE, sort), true);
        } else {
            final DistributionSetFilter distributionSetFilter = new DistributionSetFilterBuilder().setIsDeleted(false)
                    .setIsComplete(true).setSearchText(searchText).setSelectDSWithNoTag(noTagClicked)
                    .setTagNames(distributionTags).build();

            firstPageDistributionSets = getDistributionSetManagement().findByDistributionSetFilter(
                    new PageRequest(0, SPUIDefinitions.PAGE_SIZE, sort), distributionSetFilter);

        }
        final long size = firstPageDistributionSets.getTotalElements();

        if (size > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) size;
    }

    @Override
    protected void saveBeans(final List<ProxyDistribution> addedDists, final List<ProxyDistribution> modifiedDists,
            final List<ProxyDistribution> removedDists) {
        // Add,Delete and Update are performed through repository methods
    }

    @Override
    protected ProxyDistribution constructBean() {
        return new ProxyDistribution();
    }

    private DistributionSetManagement getDistributionSetManagement() {
        if (distributionSetManagement == null) {
            distributionSetManagement = SpringContextHelper.getBean(DistributionSetManagement.class);
        }
        return distributionSetManagement;
    }

}
