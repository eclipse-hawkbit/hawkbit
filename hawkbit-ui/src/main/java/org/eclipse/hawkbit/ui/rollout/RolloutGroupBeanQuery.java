/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
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
 * @author gah6kor
 *
 */
public class RolloutGroupBeanQuery extends AbstractBeanQuery<ProxyRolloutGroup> {

    private static final long serialVersionUID = 5342450502894318589L;

    private Sort sort = new Sort(Direction.ASC, "createdAt");

    private transient Page<RolloutGroup> firstPageRolloutGroupSets = null;

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

        if (HawkbitCommonUtil.checkBolArray(sortStates)) {
            // Initalize Sor
            sort = new Sort(sortStates[0] ? Direction.ASC : Direction.DESC, (String) sortPropertyIds[0]);
            // Add sort.
            for (int targetId = 1; targetId < sortPropertyIds.length; targetId++) {
                sort.and(new Sort(sortStates[targetId] ? Direction.ASC : Direction.DESC,
                        (String) sortPropertyIds[targetId]));
            }
        }
    }

    /**
     * @return
     */
    private Long getRolloutId() {
        return getRolloutUIState().getRolloutId().isPresent() ? getRolloutUIState().getRolloutId().get() : null;
    }

    @Override
    protected ProxyRolloutGroup constructBean() {
        return new ProxyRolloutGroup();
    }

    @Override
    protected List<ProxyRolloutGroup> loadBeans(final int startIndex, final int count) {
        List<RolloutGroup> proxyRolloutGroupsList = new ArrayList<>();
        if (startIndex == 0 && firstPageRolloutGroupSets != null) {
            proxyRolloutGroupsList = firstPageRolloutGroupSets.getContent();
        } else if (null != rolloutId) {
            proxyRolloutGroupsList = getRolloutGroupManagement().findAllRolloutGroupsWithDetailedStatus(rolloutId,
                    new PageRequest(startIndex / count, count)).getContent();
        }
        return getProxyRolloutGroupList(proxyRolloutGroupsList);
    }

    private List<ProxyRolloutGroup> getProxyRolloutGroupList(final List<RolloutGroup> rolloutGroupBeans) {
        final List<ProxyRolloutGroup> proxyRolloutGroupsList = new ArrayList<>();
        for (final RolloutGroup rolloutGroup : rolloutGroupBeans) {
            final ProxyRolloutGroup proxyRolloutGroup = new ProxyRolloutGroup();
            proxyRolloutGroup.setName(rolloutGroup.getName());
            proxyRolloutGroup.setDescription(rolloutGroup.getDescription());
            proxyRolloutGroup.setCreatedDate(SPDateTimeUtil.getFormattedDate(rolloutGroup.getCreatedAt()));
            proxyRolloutGroup.setModifiedDate(SPDateTimeUtil.getFormattedDate(rolloutGroup.getLastModifiedAt()));
            proxyRolloutGroup.setCreatedBy(HawkbitCommonUtil.getIMUser(rolloutGroup.getCreatedBy()));
            proxyRolloutGroup.setLastModifiedBy(HawkbitCommonUtil.getIMUser(rolloutGroup.getLastModifiedBy()));
            proxyRolloutGroup.setId(rolloutGroup.getId());
            proxyRolloutGroup.setStatus(rolloutGroup.getStatus());
            proxyRolloutGroup.setErrorAction(rolloutGroup.getErrorAction());
            proxyRolloutGroup.setErrorActionExp(rolloutGroup.getErrorActionExp());
            proxyRolloutGroup.setErrorCondition(rolloutGroup.getErrorCondition());
            proxyRolloutGroup.setErrorConditionExp(rolloutGroup.getErrorConditionExp());
            proxyRolloutGroup.setSuccessCondition(rolloutGroup.getSuccessCondition());
            proxyRolloutGroup.setSuccessConditionExp(rolloutGroup.getSuccessConditionExp());
            proxyRolloutGroup.setFinishedPercentage(calculateFinishedPercentage(rolloutGroup));

            final TotalTargetCountStatus totalTargetCountActionStatus = rolloutGroup.getTotalTargetCountStatus();

            proxyRolloutGroup.setRunningTargetsCount(totalTargetCountActionStatus
                    .getTotalTargetCountByStatus(TotalTargetCountStatus.Status.RUNNING));
            proxyRolloutGroup.setErrorTargetsCount(totalTargetCountActionStatus
                    .getTotalTargetCountByStatus(TotalTargetCountStatus.Status.ERROR));
            proxyRolloutGroup.setCancelledTargetsCount(totalTargetCountActionStatus
                    .getTotalTargetCountByStatus(TotalTargetCountStatus.Status.CANCELLED));
            proxyRolloutGroup.setFinishedTargetsCount(totalTargetCountActionStatus
                    .getTotalTargetCountByStatus(TotalTargetCountStatus.Status.FINISHED));
            proxyRolloutGroup.setScheduledTargetsCount(totalTargetCountActionStatus
                    .getTotalTargetCountByStatus(TotalTargetCountStatus.Status.SCHEDULED));
            proxyRolloutGroup.setNotStartedTargetsCount(totalTargetCountActionStatus
                    .getTotalTargetCountByStatus(TotalTargetCountStatus.Status.NOTSTARTED));

            proxyRolloutGroup.setTotalTargetsCount(String.valueOf(rolloutGroup.getTotalTargets()));

            proxyRolloutGroupsList.add(proxyRolloutGroup);
        }
        return proxyRolloutGroupsList;
    }

    private String calculateFinishedPercentage(final RolloutGroup rolloutGroup) {
        float finishedPercentage = 0;
        switch (rolloutGroup.getStatus()) {
        case READY:
        case SCHEDULED:
        case ERROR:
            finishedPercentage = 0.0F;
            break;
        case FINISHED:
            finishedPercentage = 100.0F;
            break;
        case RUNNING:
            finishedPercentage = getRolloutManagement().getFinishedPercentForRunningGroup(rolloutGroup.getRollout(),
                    rolloutGroup);
            break;
        default:
            break;
        }
        return String.valueOf(finishedPercentage);
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
        if (null != rolloutId) {
            firstPageRolloutGroupSets = getRolloutGroupManagement().findAllRolloutGroupsWithDetailedStatus(rolloutId,
                    new PageRequest(0, SPUIDefinitions.PAGE_SIZE, sort));
            size = firstPageRolloutGroupSets.getTotalElements();
        }
        if (size > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) size;
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
     * @return the rolloutManagement
     */
    public RolloutGroupManagement getRolloutGroupManagement() {
        if (null == rolloutGroupManagement) {
            rolloutGroupManagement = SpringContextHelper.getBean(RolloutGroupManagement.class);
        }
        return rolloutGroupManagement;
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
