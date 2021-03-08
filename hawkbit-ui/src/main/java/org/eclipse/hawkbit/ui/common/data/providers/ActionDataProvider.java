/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.providers;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.data.mappers.ActionToProxyActionMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.util.StringUtils;

/**
 * Data provider for {@link Action}, which dynamically loads a batch of
 * {@link Action} entities from backend and maps them to corresponding
 * {@link ProxyAction} entities. The filter is used for master-details
 * relationship with {@link Target}, using its controllerId.
 */
public class ActionDataProvider extends AbstractProxyDataProvider<ProxyAction, Action, String> {

    private static final long serialVersionUID = 1L;

    private final transient DeploymentManagement deploymentManagement;

    /**
     * Constructor for ActionDataProvider
     *
     * @param deploymentManagement
     *          DeploymentManagement
     * @param entityMapper
     *          ActionToProxyActionMapper
     */
    public ActionDataProvider(final DeploymentManagement deploymentManagement,
            final ActionToProxyActionMapper entityMapper) {
        super(entityMapper, Sort.by(Direction.DESC, "id"));

        this.deploymentManagement = deploymentManagement;
    }

    @Override
    protected Slice<Action> loadBackendEntities(final PageRequest pageRequest, final String controllerId) {
        if (StringUtils.isEmpty(controllerId)) {
            return Page.empty(pageRequest);
        }

        return deploymentManagement.findActionsByTarget(controllerId, pageRequest);
    }

    @Override
    protected long sizeInBackEnd(final PageRequest pageRequest, final String controllerId) {
        if (StringUtils.isEmpty(controllerId)) {
            return 0L;
        }

        return deploymentManagement.countActionsByTarget(controllerId);
    }
}
