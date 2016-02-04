/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.DistributionSetFilter;
import org.eclipse.hawkbit.repository.DistributionSetFilter.DistributionSetFilterBuilder;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
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

/**
 * Bean query for distribution set combo.
 *
 */
public class DistBeanQuery extends AbstractBeanQuery<ProxyDistribution> {

    private static final long serialVersionUID = 5176481314404662215L;
    private Sort sort = new Sort(Direction.ASC, "name", "version");
    private transient DistributionSetManagement distributionSetManagement;
    private transient Page<DistributionSet> firstPageDistributionSets = null;

    /**
     * Parametric Constructor.
     * 
     * @param definition
     *            as QueryDefinition
     * @param queryConfig
     *            as Config
     * @param sortPropertyIds
     *            as sort
     * @param sortStates
     *            as Sort status
     */
    public DistBeanQuery(final QueryDefinition definition, final Map<String, Object> queryConfig,
            final Object[] sortPropertyIds, final boolean[] sortStates) {
        super(definition, queryConfig, sortPropertyIds, sortStates);

        if (sortStates.length > 0) {
            // Initalize sort
            sort = new Sort(sortStates[0] ? Direction.ASC : Direction.DESC, (String) sortPropertyIds[0]);
            // Add sort
            for (int distId = 1; distId < sortPropertyIds.length; distId++) {
                sort.and(new Sort(sortStates[distId] ? Direction.ASC : Direction.DESC, (String) sortPropertyIds[distId]));
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
    protected ProxyDistribution constructBean() {
        return new ProxyDistribution();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery#loadBeans(int,
     * int)
     */
    @Override
    protected List<ProxyDistribution> loadBeans(final int startIndex, final int count) {
        Page<DistributionSet> distBeans;
        final DistributionSetFilter distributionSetFilter = new DistributionSetFilterBuilder().setIsDeleted(false)
                .build();
        if (startIndex == 0 && firstPageDistributionSets != null) {
            distBeans = firstPageDistributionSets;
        } else {
            distBeans = getDistributionSetManagement().findDistributionSetsByFilters(
                    new PageRequest(startIndex / count, count, sort), distributionSetFilter);
        }
        return createProxyDistributions(distBeans);
    }

    private List<ProxyDistribution> createProxyDistributions(final Page<DistributionSet> distBeans) {
        final List<ProxyDistribution> proxyDistributions = new ArrayList<>();
        for (final DistributionSet distributionSet : distBeans) {
            final ProxyDistribution proxyDistribution = new ProxyDistribution();
            proxyDistribution.setName(HawkbitCommonUtil.getFormattedNameVersion(distributionSet.getName(),
                    distributionSet.getVersion()));
            proxyDistribution.setDescription(distributionSet.getDescription());
            proxyDistribution.setDistId(distributionSet.getId());
            proxyDistribution.setId(distributionSet.getId());
            proxyDistribution.setVersion(distributionSet.getVersion());
            proxyDistribution.setCreatedDate(SPDateTimeUtil.getFormattedDate(distributionSet.getCreatedAt()));
            proxyDistribution.setLastModifiedDate(SPDateTimeUtil.getFormattedDate(distributionSet.getLastModifiedAt()));
            proxyDistribution.setDescription(distributionSet.getDescription());
            proxyDistribution.setCreatedByUser(HawkbitCommonUtil.getIMUser(distributionSet.getCreatedBy()));
            proxyDistribution.setModifiedByUser(HawkbitCommonUtil.getIMUser(distributionSet.getLastModifiedBy()));
            proxyDistribution.setIsComplete(distributionSet.isComplete());
            proxyDistributions.add(proxyDistribution);
        }
        return proxyDistributions;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery#saveBeans(java.
     * util.List, java.util.List, java.util.List)
     */
    @Override
    protected void saveBeans(final List<ProxyDistribution> arg0, final List<ProxyDistribution> arg1,
            final List<ProxyDistribution> arg2) {
        // Add,Delete and Update are performed through repository methods
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery#size()
     */
    @Override
    public int size() {
        final DistributionSetFilter distributionSetFilter = new DistributionSetFilterBuilder().setIsDeleted(false)
                .setIsComplete(true).build();

        firstPageDistributionSets = getDistributionSetManagement().findDistributionSetsByFilters(
                new PageRequest(0, SPUIDefinitions.PAGE_SIZE, sort), distributionSetFilter);
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
