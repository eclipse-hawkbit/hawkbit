/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rollout;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.ui.common.UserDetailsFormatter;
import org.eclipse.hawkbit.ui.customrenderers.client.renderers.RolloutRendererData;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

import com.google.common.base.Strings;

/**
 *
 * Simple implementation of generics bean query which dynamically loads a batch
 * of {@link ProxyRollout} beans.
 *
 */
public class RolloutBeanQuery extends AbstractBeanQuery<ProxyRollout> {

    private static final long serialVersionUID = 4027879794344836185L;

    private final String searchText;

    private Sort sort = new Sort(Direction.ASC, "id");

    private transient RolloutManagement rolloutManagement;

    private transient TargetFilterQueryManagement filterQueryManagement;

    private transient RolloutUIState rolloutUIState;

    /**
     * Parametric Constructor.
     *
     * @param definition
     *            as QueryDefinition
     * @param queryConfig
     *            as Config
     * @param sortIds
     *            as sort
     * @param sortStates
     *            as Sort status
     */
    public RolloutBeanQuery(final QueryDefinition definition, final Map<String, Object> queryConfig,
            final Object[] sortIds, final boolean[] sortStates) {
        super(definition, queryConfig, sortIds, sortStates);

        searchText = getSearchText();

        if (!isEmpty(sortStates)) {

            sort = new Sort(sortStates[0] ? Direction.ASC : Direction.DESC, (String) sortIds[0]);

            for (int targetId = 1; targetId < sortIds.length; targetId++) {
                sort.and(new Sort(sortStates[targetId] ? Direction.ASC : Direction.DESC, (String) sortIds[targetId]));
            }
        }
    }

    private String getSearchText() {
        return getRolloutUIState().getSearchText().map(value -> String.format("%%%s%%", value)).orElse(null);
    }

    @Override
    protected ProxyRollout constructBean() {
        return new ProxyRollout();
    }

    @Override
    protected List<ProxyRollout> loadBeans(final int startIndex, final int count) {
        final Slice<Rollout> rolloutBeans;
        if (Strings.isNullOrEmpty(searchText)) {
            rolloutBeans = getRolloutManagement().findAllRolloutsWithDetailedStatus(
                    new PageRequest(startIndex / SPUIDefinitions.PAGE_SIZE, SPUIDefinitions.PAGE_SIZE, sort));
        } else {
            rolloutBeans = getRolloutManagement().findRolloutByFilters(
                    new PageRequest(startIndex / SPUIDefinitions.PAGE_SIZE, SPUIDefinitions.PAGE_SIZE, sort),
                    searchText);
        }
        return getProxyRolloutList(rolloutBeans);
    }

    private List<ProxyRollout> getProxyRolloutList(final Slice<Rollout> rolloutBeans) {
        final List<ProxyRollout> proxyRolloutList = new ArrayList<>();
        for (final Rollout rollout : rolloutBeans) {
            final ProxyRollout proxyRollout = new ProxyRollout();
            proxyRollout.setName(rollout.getName());
            proxyRollout.setDescription(rollout.getDescription());
            final DistributionSet distributionSet = rollout.getDistributionSet();
            proxyRollout.setDistributionSetNameVersion(
                    HawkbitCommonUtil.getFormattedNameVersion(distributionSet.getName(), distributionSet.getVersion()));
            proxyRollout.setDistributionSet(distributionSet);
            proxyRollout.setNumberOfGroups(Long.valueOf(rollout.getRolloutGroups().size()));
            proxyRollout.setCreatedDate(SPDateTimeUtil.getFormattedDate(rollout.getCreatedAt()));
            proxyRollout.setModifiedDate(SPDateTimeUtil.getFormattedDate(rollout.getLastModifiedAt()));
            proxyRollout.setCreatedBy(UserDetailsFormatter.loadAndFormatCreatedBy(rollout));
            proxyRollout.setLastModifiedBy(UserDetailsFormatter.loadAndFormatLastModifiedBy(rollout));
            proxyRollout.setForcedTime(rollout.getForcedTime());
            proxyRollout.setId(rollout.getId());
            proxyRollout.setStatus(rollout.getStatus());
            proxyRollout
                    .setRolloutRendererData(new RolloutRendererData(rollout.getName(), rollout.getStatus().toString()));

            final TotalTargetCountStatus totalTargetCountActionStatus = rollout.getTotalTargetCountStatus();
            proxyRollout.setTotalTargetCountStatus(totalTargetCountActionStatus);
            proxyRollout.setTotalTargetsCount(String.valueOf(rollout.getTotalTargets()));
            proxyRollout.setType(distributionSet.getType().getName());
            proxyRollout.setIsRequiredMigrationStep(distributionSet.isRequiredMigrationStep());
            proxyRollout.setSwModules(distributionSet.getModules());

            proxyRolloutList.add(proxyRollout);
        }
        return proxyRolloutList;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery#saveBeans(java
     * .util.List, java.util.List, java.util.List)
     */
    @Override
    protected void saveBeans(final List<ProxyRollout> arg0, final List<ProxyRollout> arg1,
            final List<ProxyRollout> arg2) {
        /**
         * CRUD operations on Target will be done through repository methods
         */
    }

    @Override
    public int size() {
        int size = getRolloutManagement().countRolloutsAll().intValue();
        if (!Strings.isNullOrEmpty(searchText)) {
            size = getRolloutManagement().countRolloutsAllByFilters(searchText).intValue();
        }
        return size;
    }

    /**
     * @return the rolloutManagement
     */
    public RolloutManagement getRolloutManagement() {
        if (null == rolloutManagement) {
            rolloutManagement = SpringContextHelper.getBean(RolloutManagement.class);
        }
        return rolloutManagement;
    }

    /**
     * @return the filterQueryManagement
     */
    public TargetFilterQueryManagement getFilterQueryManagement() {
        if (null == filterQueryManagement) {
            filterQueryManagement = SpringContextHelper.getBean(TargetFilterQueryManagement.class);
        }
        return filterQueryManagement;
    }

    /**
     * @return the rolloutUIState
     */
    public RolloutUIState getRolloutUIState() {
        if (null == rolloutUIState) {
            rolloutUIState = SpringContextHelper.getBean(RolloutUIState.class);
        }
        return rolloutUIState;
    }

}
