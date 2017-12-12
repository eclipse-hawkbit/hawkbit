/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rolloutgrouptargets;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetWithActionStatus;
import org.eclipse.hawkbit.ui.common.UserDetailsFormatter;
import org.eclipse.hawkbit.ui.components.ProxyTarget;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
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
 * of {@link ProxyTarget} beans.
 */
public class RolloutGroupTargetsBeanQuery extends AbstractBeanQuery<ProxyTarget> {

    private static final long serialVersionUID = -8841076207255485907L;

    private static final Logger LOG = LoggerFactory.getLogger(RolloutGroupTargetsBeanQuery.class);

    private static final Sort sort = new Sort(Direction.ASC, "id");

    private transient Page<TargetWithActionStatus> firstPageTargetSets;

    private transient RolloutManagement rolloutManagement;

    private transient RolloutGroupManagement rolloutGroupManagement;

    private transient RolloutUIState rolloutUIState;

    private final transient Optional<RolloutGroup> rolloutGroup;

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
    public RolloutGroupTargetsBeanQuery(final QueryDefinition definition, final Map<String, Object> queryConfig,
            final Object[] sortPropertyIds, final boolean[] sortStates) {

        super(definition, queryConfig, sortPropertyIds, sortStates);
        rolloutGroup = getRolloutUIState().getRolloutGroup();
    }

    @Override
    protected ProxyTarget constructBean() {
        return new ProxyTarget();
    }

    @Override
    protected List<ProxyTarget> loadBeans(final int startIndex, final int count) {
        if (startIndex == 0 && firstPageTargetSets != null) {
            return getProxyRolloutGroupTargetsList(firstPageTargetSets.getContent());
        }

        return rolloutGroup.map(group -> getProxyRolloutGroupTargetsList(getRolloutGroupManagement()
                .findAllTargetsOfRolloutGroupWithActionStatus(new PageRequest(startIndex / count, count), group.getId())
                .getContent())).orElse(Collections.emptyList());
    }

    private static List<ProxyTarget> getProxyRolloutGroupTargetsList(
            final List<TargetWithActionStatus> rolloutGroupTargets) {

        return rolloutGroupTargets.stream().map(RolloutGroupTargetsBeanQuery::mapTargetToProxy)
                .collect(Collectors.toList());
    }

    private static ProxyTarget mapTargetToProxy(final TargetWithActionStatus targetWithActionStatus) {
        final Target targ = targetWithActionStatus.getTarget();
        final ProxyTarget prxyTarget = new ProxyTarget();
        prxyTarget.setName(targ.getName());
        prxyTarget.setDescription(targ.getDescription());
        prxyTarget.setControllerId(targ.getControllerId());
        prxyTarget.setInstallationDate(targ.getInstallationDate());
        prxyTarget.setAddress(targ.getAddress());
        prxyTarget.setLastTargetQuery(targ.getLastTargetQuery());
        prxyTarget.setLastModifiedDate(SPDateTimeUtil.getFormattedDate(targ.getLastModifiedAt()));
        prxyTarget.setCreatedDate(SPDateTimeUtil.getFormattedDate(targ.getCreatedAt()));
        prxyTarget.setCreatedAt(targ.getCreatedAt());
        prxyTarget.setCreatedByUser(UserDetailsFormatter.loadAndFormatCreatedBy(targ));
        prxyTarget.setModifiedByUser(UserDetailsFormatter.loadAndFormatLastModifiedBy(targ));
        if (targetWithActionStatus.getStatus() != null) {
            prxyTarget.setStatus(targetWithActionStatus.getStatus());
        }
        prxyTarget.setLastTargetQuery(targ.getLastTargetQuery());
        prxyTarget.setId(targ.getId());
        return prxyTarget;
    }

    @Override
    protected void saveBeans(final List<ProxyTarget> arg0, final List<ProxyTarget> arg1, final List<ProxyTarget> arg2) {
        /**
         * No implementation required.
         */
    }

    @Override
    public int size() {
        long size = 0;

        try {
            firstPageTargetSets = rolloutGroup.map(group -> getRolloutGroupManagement()
                    .findAllTargetsOfRolloutGroupWithActionStatus(new PageRequest(0, SPUIDefinitions.PAGE_SIZE, sort), group.getId()))
                    .orElse(null);

            size = firstPageTargetSets == null ? 0 : firstPageTargetSets.getTotalElements();
        } catch (final EntityNotFoundException e) {
            LOG.error("Rollout does not exists. Redirect to Rollouts overview", e);
            rolloutUIState.setShowRolloutGroupTargets(false);
            rolloutUIState.setShowRollOuts(true);
            return 0;
        }

        getRolloutUIState().setRolloutGroupTargetsTotalCount(size);
        if (size > SPUIDefinitions.MAX_TABLE_ENTRIES) {
            getRolloutUIState().setRolloutGroupTargetsTruncated(size - SPUIDefinitions.MAX_TABLE_ENTRIES);
            return SPUIDefinitions.MAX_TABLE_ENTRIES;
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
