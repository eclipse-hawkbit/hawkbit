/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.actionhistory;

import static org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil.isNotNullOrEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.ui.management.actionhistory.ProxyAction.IsActiveDecoration;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

/**
 * Simple implementation of generics bean query which dynamically loads
 * {@link ProxyAction} batch of beans.
 *
 */
public class ActionBeanQuery extends AbstractBeanQuery<ProxyAction> {
    private static final long serialVersionUID = 3596912494728552516L;

    private Sort sort = new Sort(Direction.DESC, ProxyAction.PXY_ACTION_ID);
    private transient DeploymentManagement deploymentManagement;

    private String currentSelectedConrollerId;
    private transient Slice<Action> firstPageActions;

    /**
     * Parametric Constructor.
     *
     * @param definition
     *            QueryDefinition contains the query properties.
     * @param queryConfig
     *            Implementation specific configuration.
     * @param sortPropertyIds
     *            The properties participating in sort.
     * @param sortStates
     *            The ascending or descending state of sort properties.
     */
    public ActionBeanQuery(final QueryDefinition definition, final Map<String, Object> queryConfig,
            final Object[] sortPropertyIds, final boolean[] sortStates) {
        super(definition, queryConfig, sortPropertyIds, sortStates);

        if (isNotNullOrEmpty(queryConfig)) {
            currentSelectedConrollerId = (String) queryConfig.get(SPUIDefinitions.ACTIONS_BY_TARGET);
        }

        if (sortStates == null || sortStates.length <= 0) {
            return;
        }

        // Initialize sort
        sort = new Sort(sortStates[0] ? Direction.ASC : Direction.DESC, (String) sortPropertyIds[0]);
        // Add sort
        for (int distId = 1; distId < sortPropertyIds.length; distId++) {
            sort.and(new Sort(sortStates[distId] ? Direction.ASC : Direction.DESC, (String) sortPropertyIds[distId]));
        }
    }

    @Override
    protected ProxyAction constructBean() {
        return new ProxyAction();
    }

    @Override
    protected List<ProxyAction> loadBeans(final int startIndex, final int count) {
        Slice<Action> actionBeans;
        if (startIndex == 0) {
            if (firstPageActions == null) {
                firstPageActions = getDeploymentManagement().findActionsByTarget(currentSelectedConrollerId,
                        new PageRequest(0, SPUIDefinitions.PAGE_SIZE, sort));
            }
            actionBeans = firstPageActions;
        } else {
            actionBeans = getDeploymentManagement().findActionsByTarget(currentSelectedConrollerId,
                    new PageRequest(startIndex / SPUIDefinitions.PAGE_SIZE, SPUIDefinitions.PAGE_SIZE, sort));
        }
        return createProxyActions(actionBeans);
    }

    /**
     * Creates a list of {@link ProxyAction}s for presentation layer from slice
     * of {@link Action}s.
     *
     * @param actionBeans
     *            slice of {@link Action}s
     * @return list of {@link ProxyAction}s
     */
    private static List<ProxyAction> createProxyActions(final Slice<Action> actionBeans) {
        final List<ProxyAction> proxyActions = new ArrayList<>();
        for (final Action action : actionBeans) {
            final ProxyAction proxyAction = new ProxyAction();
            final String dsNameVersion = action.getDistributionSet().getName() + ":"
                    + action.getDistributionSet().getVersion();
            proxyAction.setActive(action.isActive());
            proxyAction.setIsActiveDecoration(buildIsActiveDecoration(action));
            proxyAction.setDsNameVersion(dsNameVersion);
            proxyAction.setAction(action);
            proxyAction.setId(action.getId());
            proxyAction.setLastModifiedAt(action.getLastModifiedAt());
            proxyAction.setRolloutName(action.getRollout() != null ? action.getRollout().getName() : "");
            proxyAction.setStatus(action.getStatus());

            proxyActions.add(proxyAction);
        }
        return proxyActions;
    }

    /**
     * Generates a virtual IsActiveDecoration for the presentation layer that is
     * calculated from {@link Action#isActive()} and
     * {@link Action#getActionStatus()}.
     *
     * @param action
     *            the action combined IsActiveDecoration is calculated from
     * @return IsActiveDecoration combined decoration for the presentation
     *         layer.
     */
    private static IsActiveDecoration buildIsActiveDecoration(final Action action) {
        final Action.Status status = action.getStatus();

        if (status == Action.Status.SCHEDULED) {
            return IsActiveDecoration.SCHEDULED;
        } else if (status == Action.Status.ERROR) {
            return IsActiveDecoration.IN_ACTIVE_ERROR;
        }

        return action.isActive() ? IsActiveDecoration.ACTIVE : IsActiveDecoration.IN_ACTIVE;
    }

    @Override
    protected void saveBeans(final List<ProxyAction> addedBeans, final List<ProxyAction> modifiedBeans,
            final List<ProxyAction> removedBeans) {
        // CRUD operations on Target will be done through repository methods
    }

    @Override
    public int size() {
        long size = 0;

        if (currentSelectedConrollerId != null) {
            size = getDeploymentManagement().countActionsByTarget(currentSelectedConrollerId);
        }
        if (size > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) size;
    }

    /**
     * Lazy gets deploymentManagement.
     *
     * @return the deploymentManagement
     */
    public DeploymentManagement getDeploymentManagement() {
        if (null == deploymentManagement) {
            deploymentManagement = SpringContextHelper.getBean(DeploymentManagement.class);
        }
        return deploymentManagement;
    }

}
