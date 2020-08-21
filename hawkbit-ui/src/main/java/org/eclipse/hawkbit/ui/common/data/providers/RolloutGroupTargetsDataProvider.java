/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.providers;

import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.model.TargetWithActionStatus;
import org.eclipse.hawkbit.ui.common.data.mappers.TargetWithActionStatusToProxyTargetMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

/**
 * Data provider for {@link TargetWithActionStatus}, which dynamically loads a
 * batch of {@link TargetWithActionStatus} entities from backend and maps them
 * to corresponding {@link ProxyTarget} entities.
 */
public class RolloutGroupTargetsDataProvider extends AbstractProxyDataProvider<ProxyTarget, TargetWithActionStatus, Long> {
    private static final long serialVersionUID = 1L;

    private final transient RolloutGroupManagement rolloutGroupManagement;

    /**
     * Constructor for RolloutGroupTargetsDataProvider
     *
     * @param rolloutGroupManagement
     *          RolloutGroupManagement
     * @param entityMapper
     *          TargetWithActionStatusToProxyTargetMapper
     */
    public RolloutGroupTargetsDataProvider(final RolloutGroupManagement rolloutGroupManagement,
            final TargetWithActionStatusToProxyTargetMapper entityMapper) {
        super(entityMapper);

        this.rolloutGroupManagement = rolloutGroupManagement;
    }

    @Override
    protected Slice<TargetWithActionStatus> loadBackendEntities(final PageRequest pageRequest,
            final Long rolloutGroupId) {
        if (rolloutGroupId == null) {
            return Page.empty(pageRequest);
        }

        return rolloutGroupManagement.findAllTargetsOfRolloutGroupWithActionStatus(pageRequest, rolloutGroupId);
    }

    @Override
    protected long sizeInBackEnd(final PageRequest pageRequest, final Long rolloutGroupId) {
        if (rolloutGroupId == null) {
            return 0L;
        }

        return rolloutGroupManagement.countTargetsOfRolloutsGroup(rolloutGroupId);
    }
}
