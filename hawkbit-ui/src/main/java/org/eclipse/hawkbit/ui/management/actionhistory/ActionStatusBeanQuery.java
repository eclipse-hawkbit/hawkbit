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
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

/**
 * Simple implementation of generics bean query which dynamically loads
 * {@link ProxyActionStatus} batch of beans.
 */
public class ActionStatusBeanQuery extends AbstractBeanQuery<ProxyActionStatus> {
    private static final long serialVersionUID = 1L;

    private Sort sort = new Sort(Direction.DESC, "id");
    private transient DeploymentManagement deploymentManagement;

    private Long currentSelectedActionId;
    private transient Page<ActionStatus> firstPageActionStates;

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
    public ActionStatusBeanQuery(final QueryDefinition definition, final Map<String, Object> queryConfig,
            final Object[] sortPropertyIds, final boolean[] sortStates) {
        super(definition, queryConfig, sortPropertyIds, sortStates);

        if (isNotNullOrEmpty(queryConfig)) {
            currentSelectedActionId = (Long) queryConfig.get(SPUIDefinitions.ACTIONSTATES_BY_ACTION);
        }

        if (sortStates!= null && sortStates.length > 0) {
            // Initialize sort
            sort = new Sort(sortStates[0] ? Direction.ASC : Direction.DESC, (String) sortPropertyIds[0]);
            // Add sort
            for (int distId = 1; distId < sortPropertyIds.length; distId++) {
                sort.and(new Sort(sortStates[distId] ? Direction.ASC : Direction.DESC,
                        (String) sortPropertyIds[distId]));
            }
        }
    }

    @Override
    protected ProxyActionStatus constructBean() {
        return new ProxyActionStatus();
    }

    @Override
    protected List<ProxyActionStatus> loadBeans(final int startIndex, final int count) {
        Page<ActionStatus> actionBeans;
        if (startIndex == 0 && firstPageActionStates != null) {
            actionBeans = firstPageActionStates;
        } else {
            actionBeans = getDeploymentManagement()
                    .findActionStatusByAction(
                            new PageRequest(startIndex / SPUIDefinitions.PAGE_SIZE, SPUIDefinitions.PAGE_SIZE, sort),
                            currentSelectedActionId);
        }
        return createProxyActionStates(actionBeans);
    }

    /**
     * Creates a list of {@link ProxyActionStatus} for presentation layer from
     * page of {@link ActionStatus}.
     *
     * @param actionBeans
     *            page of {@link ActionStatus}
     * @return list of {@link ProxyActionStatus}
     */
    private static List<ProxyActionStatus> createProxyActionStates(final Page<ActionStatus> actionStatusBeans) {
        final List<ProxyActionStatus> proxyActionStates = new ArrayList<>();
        for (final ActionStatus actionStatus : actionStatusBeans) {
            final ProxyActionStatus proxyAS = new ProxyActionStatus();
            proxyAS.setCreatedAt(actionStatus.getCreatedAt());
            proxyAS.setStatus(actionStatus.getStatus());
            proxyAS.setId(actionStatus.getId());

            proxyActionStates.add(proxyAS);
        }
        return proxyActionStates;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery#saveBeans(java
     * .util.List, java.util.List, java.util.List)
     */
    @Override
    protected void saveBeans(List<ProxyActionStatus> addedBeans, List<ProxyActionStatus> modifiedBeans,
            List<ProxyActionStatus> removedBeans) {
        // CRUD operations on Target will be done through repository methods
    }

    @Override
    public int size() {
        long size = 0;

        if (currentSelectedActionId != null) {
            firstPageActionStates = getDeploymentManagement().findActionStatusByAction(
                    new PageRequest(0, SPUIDefinitions.PAGE_SIZE, sort), currentSelectedActionId);
            size = firstPageActionStates.getTotalElements();
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
