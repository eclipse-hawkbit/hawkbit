/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rolloutgroup;

import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.ui.common.UserDetailsFormatter;
import org.eclipse.hawkbit.ui.customrenderers.client.renderers.RolloutRendererData;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

/**
 * Simple implementation of generics bean query which dynamically loads a batch
 * of {@link ProxyRolloutGroup} beans.
 *
 */
public class RolloutGroupBeanQuery extends AbstractBeanQuery<ProxyRolloutGroup> {

    private static final long serialVersionUID = 5342450502894318589L;

    private static final Logger LOG = LoggerFactory.getLogger(RolloutGroupBeanQuery.class);

    private Sort sort = new Sort(Direction.ASC, "id");

    private transient Page<RolloutGroup> firstPageRolloutGroupSets;

    private transient RolloutManagement rolloutManagement;

    private transient RolloutGroupManagement rolloutGroupManagement;

    private transient RolloutUIState rolloutUIState;

    private final Long rolloutId;

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
    public RolloutGroupBeanQuery(final QueryDefinition definition, final Map<String, Object> queryConfig,
            final Object[] sortPropertyIds, final boolean[] sortStates) {

        super(definition, queryConfig, sortPropertyIds, sortStates);

        rolloutId = getRolloutId();

        if (sortStates != null && sortStates.length > 0) {

            sort = new Sort(sortStates[0] ? ASC : DESC, (String) sortPropertyIds[0]);

            for (int targetId = 1; targetId < sortPropertyIds.length; targetId++) {
                sort.and(new Sort(sortStates[targetId] ? ASC : DESC, (String) sortPropertyIds[targetId]));
            }
        }
    }

    private Long getRolloutId() {
        return getRolloutUIState().getRolloutId().orElse(null);
    }

    @Override
    protected ProxyRolloutGroup constructBean() {
        return new ProxyRolloutGroup();
    }

    @Override
    protected List<ProxyRolloutGroup> loadBeans(final int startIndex, final int count) {
        List<RolloutGroup> proxyRolloutGroupsList = new ArrayList<>();
        if (rolloutId != null) {
            if (startIndex == 0 && firstPageRolloutGroupSets != null) {
                proxyRolloutGroupsList = firstPageRolloutGroupSets.getContent();
            } else {
                proxyRolloutGroupsList = getRolloutGroupManagement()
                        .findByRolloutWithDetailedStatus(new PageRequest(startIndex / count, count), rolloutId)
                        .getContent();
            }
        }
        return getProxyRolloutGroupList(proxyRolloutGroupsList);
    }

    private List<ProxyRolloutGroup> getProxyRolloutGroupList(final List<RolloutGroup> rolloutGroupBeans) {
        return rolloutGroupBeans.stream().map(RolloutGroupBeanQuery::createProxy).collect(Collectors.toList());
    }

    private static ProxyRolloutGroup createProxy(final RolloutGroup rolloutGroup) {
        final ProxyRolloutGroup proxyRolloutGroup = new ProxyRolloutGroup();
        proxyRolloutGroup.setName(rolloutGroup.getName());
        proxyRolloutGroup.setDescription(rolloutGroup.getDescription());
        proxyRolloutGroup.setCreatedDate(SPDateTimeUtil.getFormattedDate(rolloutGroup.getCreatedAt()));
        proxyRolloutGroup.setModifiedDate(SPDateTimeUtil.getFormattedDate(rolloutGroup.getLastModifiedAt()));
        proxyRolloutGroup.setCreatedBy(UserDetailsFormatter.loadAndFormatCreatedBy(rolloutGroup));
        proxyRolloutGroup.setLastModifiedBy(UserDetailsFormatter.loadAndFormatLastModifiedBy(rolloutGroup));
        proxyRolloutGroup.setId(rolloutGroup.getId());
        proxyRolloutGroup.setStatus(rolloutGroup.getStatus());
        proxyRolloutGroup.setErrorAction(rolloutGroup.getErrorAction());
        proxyRolloutGroup.setErrorActionExp(rolloutGroup.getErrorActionExp());
        proxyRolloutGroup.setErrorCondition(rolloutGroup.getErrorCondition());
        proxyRolloutGroup.setErrorConditionExp(rolloutGroup.getErrorConditionExp());
        proxyRolloutGroup.setSuccessCondition(rolloutGroup.getSuccessCondition());
        proxyRolloutGroup.setSuccessConditionExp(rolloutGroup.getSuccessConditionExp());
        proxyRolloutGroup.setFinishedPercentage(HawkbitCommonUtil.formattingFinishedPercentage(rolloutGroup,
                rolloutGroup.getTotalTargetCountStatus().getFinishedPercent()));

        proxyRolloutGroup.setRolloutRendererData(new RolloutRendererData(rolloutGroup.getName(), null));

        proxyRolloutGroup.setTotalTargetsCount(String.valueOf(rolloutGroup.getTotalTargets()));
        proxyRolloutGroup.setTotalTargetCountStatus(rolloutGroup.getTotalTargetCountStatus());

        return proxyRolloutGroup;
    }

    @Override
    protected void saveBeans(final List<ProxyRolloutGroup> arg0, final List<ProxyRolloutGroup> arg1,
            final List<ProxyRolloutGroup> arg2) {
        /**
         * CRUD operations be done through repository methods.
         */
    }

    @Override
    public int size() {
        long size = 0;
        if (rolloutId != null) {
            try {
                firstPageRolloutGroupSets = getRolloutGroupManagement().findByRolloutWithDetailedStatus(
                        new PageRequest(0, SPUIDefinitions.PAGE_SIZE, sort), rolloutId);
                size = firstPageRolloutGroupSets.getTotalElements();
            } catch (final EntityNotFoundException e) {
                LOG.error("Rollout does not exists. Redirect to Rollouts overview", e);
                rolloutUIState.setShowRolloutGroups(false);
                rolloutUIState.setShowRollOuts(true);
                return 0;
            }
        }
        if (size > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) size;
    }

    public RolloutManagement getRolloutManagement() {
        if (rolloutManagement == null) {
            rolloutManagement = SpringContextHelper.getBean(RolloutManagement.class);
        }
        return rolloutManagement;
    }

    public RolloutGroupManagement getRolloutGroupManagement() {
        if (rolloutGroupManagement == null) {
            rolloutGroupManagement = SpringContextHelper.getBean(RolloutGroupManagement.class);
        }
        return rolloutGroupManagement;
    }

    public RolloutUIState getRolloutUIState() {
        if (rolloutUIState == null) {
            rolloutUIState = SpringContextHelper.getBean(RolloutUIState.class);
        }
        return rolloutUIState;
    }

}
