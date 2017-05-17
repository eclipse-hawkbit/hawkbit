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
import org.springframework.util.StringUtils;
import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

/**
 * Simple implementation of generic bean query which dynamically loads
 * {@link ProxyMessage} batch of beans.
 */
public class ActionStatusMsgBeanQuery extends AbstractBeanQuery<ProxyMessage> {
    private static final long serialVersionUID = 1L;

    private Sort sort = new Sort(Direction.DESC, "id");
    private transient DeploymentManagement deploymentManagement;

    private Long currentSelectedActionStatusId;
    private String noMessageText;
    private transient Page<String> firstPageMessages;

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
    public ActionStatusMsgBeanQuery(final QueryDefinition definition, final Map<String, Object> queryConfig,
            final Object[] sortPropertyIds, final boolean[] sortStates) {
        super(definition, queryConfig, sortPropertyIds, sortStates);

        if (isNotNullOrEmpty(queryConfig)) {
            currentSelectedActionStatusId = (Long) queryConfig.get(SPUIDefinitions.MESSAGES_BY_ACTIONSTATUS);
            noMessageText = (String) queryConfig.get(SPUIDefinitions.NO_MSG_PROXY);
        }

        if (sortStates != null && sortStates.length > 0) {
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
    protected ProxyMessage constructBean() {
        return new ProxyMessage();
    }

    @Override
    protected List<ProxyMessage> loadBeans(final int startIndex, final int count) {
        Page<String> actionBeans;
        if (startIndex == 0 && firstPageMessages != null) {
            actionBeans = firstPageMessages;
        } else {
            actionBeans = getDeploymentManagement().findMessagesByActionStatusId(
                    new PageRequest(startIndex / SPUIDefinitions.PAGE_SIZE, SPUIDefinitions.PAGE_SIZE, sort),
                    currentSelectedActionStatusId);
        }
        return createProxyMessages(actionBeans);
    }

    /**
     * Creates a list of {@link ProxyActionStatus} for presentation layer from
     * page of {@link ActionStatus}.
     *
     * @param actionBeans
     *            page of {@link ActionStatus}
     * @return list of {@link ProxyActionStatus}
     */
    private List<ProxyMessage> createProxyMessages(final Page<String> messages) {
        final List<ProxyMessage> proxyMsgs = new ArrayList<>(messages.getNumberOfElements());

        Long idx = messages.getNumber() * ((long) messages.getSize());
        for (final String msg : messages) {
            idx++;
            final ProxyMessage proxyMsg = new ProxyMessage();
            proxyMsg.setMessage(msg);
            proxyMsg.setId(String.valueOf(idx));
            proxyMsgs.add(proxyMsg);
        }

        if (messages.getTotalElements() == 1L && StringUtils.isEmpty(proxyMsgs.get(0).getMessage())) {
            proxyMsgs.get(0).setMessage(noMessageText);
        }

        return proxyMsgs;
    }

    @Override
    protected void saveBeans(final List<ProxyMessage> addedBeans, final List<ProxyMessage> modifiedBeans,
            final List<ProxyMessage> removedBeans) {
        // CRUD operations on Target will be done through repository methods
    }

    @Override
    public int size() {
        long size = 0;

        if (currentSelectedActionStatusId != null) {
            firstPageMessages = getDeploymentManagement().findMessagesByActionStatusId(
                    new PageRequest(0, SPUIDefinitions.PAGE_SIZE, sort), currentSelectedActionStatusId);
            size = firstPageMessages.getTotalElements();
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
