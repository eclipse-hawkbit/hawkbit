/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.DistributionSetFilter;
import org.eclipse.hawkbit.repository.DistributionSetFilter.DistributionSetFilterBuilder;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
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
 * Simple implementation of generics bean query which dynamically loads a batch
 * of beans.
 *
 *
 *
 *
 *
 *
 *
 */
public class DistributionBeanQuery extends AbstractBeanQuery<ProxyDistribution> {

    private static final long serialVersionUID = 5862679853949173536L;
    private Sort sort = new Sort(Direction.ASC, "name", "version");
    private Collection<String> distributionTags = null;
    private String searchText = null;
    private String pinnedControllerId = null;
    private transient DistributionSetManagement distributionSetManagement;
    private transient Page<DistributionSet> firstPageDistributionSets = null;
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

        if (HawkbitCommonUtil.mapCheckStrKey(queryConfig)) {
            distributionTags = (Collection<String>) queryConfig.get(SPUIDefinitions.FILTER_BY_TAG);
            searchText = (String) queryConfig.get(SPUIDefinitions.FILTER_BY_TEXT);
            noTagClicked = (Boolean) queryConfig.get(SPUIDefinitions.FILTER_BY_NO_TAG);
            pinnedControllerId = (String) queryConfig.get(SPUIDefinitions.ORDER_BY_PINNED_TARGET);
            if (!Strings.isNullOrEmpty(searchText)) {
                searchText = String.format("%%%s%%", searchText);
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

    /**
     * Load all the Distribution set.
     * 
     * @parm startIndex as page start
     * @param count
     *            as total data
     */
    @Override
    protected List<ProxyDistribution> loadBeans(final int startIndex, final int count) {
        Page<DistributionSet> distBeans;
        final List<ProxyDistribution> proxyDistributions = new ArrayList<ProxyDistribution>();
        if (startIndex == 0 && firstPageDistributionSets != null) {
            distBeans = firstPageDistributionSets;
        } else if (pinnedControllerId != null) {
            final DistributionSetFilterBuilder distributionSetFilterBuilder = new DistributionSetFilterBuilder()
                    .setIsDeleted(false).setIsComplete(true).setSearchText(searchText)
                    .setSelectDSWithNoTag(noTagClicked).setTagNames(distributionTags);

            distBeans = getDistributionSetManagement().findDistributionSetsAllOrderedByLinkTarget(
                    new OffsetBasedPageRequest(startIndex, count, sort), distributionSetFilterBuilder,
                    pinnedControllerId);
        } else if (distributionTags.isEmpty() && Strings.isNullOrEmpty(searchText) && !noTagClicked) {
            // if no search filters available
            distBeans = getDistributionSetManagement()
                    .findDistributionSetsAll(new OffsetBasedPageRequest(startIndex, count, sort), false, true);
        } else {
            final DistributionSetFilter distributionSetFilter = new DistributionSetFilterBuilder().setIsDeleted(false)
                    .setIsComplete(true).setSearchText(searchText).setSelectDSWithNoTag(noTagClicked)
                    .setTagNames(distributionTags).build();
            distBeans = getDistributionSetManagement().findDistributionSetsByFilters(
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
            proxyDistribution.setDescription(distributionSet.getDescription());
            proxyDistribution.setCreatedByUser(HawkbitCommonUtil.getIMUser(distributionSet.getCreatedBy()));
            proxyDistribution.setModifiedByUser(HawkbitCommonUtil.getIMUser(distributionSet.getLastModifiedBy()));
            proxyDistributions.add(proxyDistribution);
        }
        return proxyDistributions;
    }

    @Override
    public int size() {
        if (pinnedControllerId != null) {

            final DistributionSetFilterBuilder distributionSetFilterBuilder = new DistributionSetFilterBuilder()
                    .setIsDeleted(false).setIsComplete(true).setSearchText(searchText)
                    .setSelectDSWithNoTag(noTagClicked).setTagNames(distributionTags);

            firstPageDistributionSets = getDistributionSetManagement().findDistributionSetsAllOrderedByLinkTarget(
                    new PageRequest(0, SPUIDefinitions.PAGE_SIZE, sort), distributionSetFilterBuilder,
                    pinnedControllerId);
        } else if (distributionTags.isEmpty() && Strings.isNullOrEmpty(searchText) && !noTagClicked) {
            // if no search filters available
            firstPageDistributionSets = getDistributionSetManagement()
                    .findDistributionSetsAll(new PageRequest(0, SPUIDefinitions.PAGE_SIZE, sort), false, true);
        } else {
            final DistributionSetFilter distributionSetFilter = new DistributionSetFilterBuilder().setIsDeleted(false)
                    .setIsComplete(true).setSearchText(searchText).setSelectDSWithNoTag(noTagClicked)
                    .setTagNames(distributionTags).build();

            firstPageDistributionSets = getDistributionSetManagement().findDistributionSetsByFilters(
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

    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(firstPageDistributionSets);
    }

    @SuppressWarnings("unchecked")
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        firstPageDistributionSets = (Page<DistributionSet>) in.readObject();
    }

}
