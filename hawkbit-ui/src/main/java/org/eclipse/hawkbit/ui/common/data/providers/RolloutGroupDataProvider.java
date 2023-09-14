/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.data.providers;

import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.ui.common.data.mappers.RolloutGroupToProxyRolloutGroupMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRolloutGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

/**
 * Data provider for {@link RolloutGroup}, which dynamically loads a batch of
 * {@link RolloutGroup} entities from backend and maps them to corresponding
 * {@link ProxyRolloutGroup} entities.
 */
public class RolloutGroupDataProvider extends AbstractProxyDataProvider<ProxyRolloutGroup, RolloutGroup, Long> {
    private static final long serialVersionUID = 1L;

    private final transient RolloutGroupManagement rolloutGroupManagement;

    /**
     * Parametric Constructor.
     *
     * @param rolloutGroupManagement
     *            rollout group management
     * @param entityMapper
     *          RolloutGroupToProxyRolloutGroupMapper
     */
    public RolloutGroupDataProvider(final RolloutGroupManagement rolloutGroupManagement,
            final RolloutGroupToProxyRolloutGroupMapper entityMapper) {
        super(entityMapper);

        this.rolloutGroupManagement = rolloutGroupManagement;
    }

    @Override
    protected Slice<RolloutGroup> loadBackendEntities(final PageRequest pageRequest, final Long rolloutId) {
        if (rolloutId == null) {
            return Page.empty(pageRequest);
        }

        return rolloutGroupManagement.findByRolloutWithDetailedStatus(pageRequest, rolloutId);

    }

    @Override
    protected long sizeInBackEnd(final PageRequest pageRequest, final Long rolloutId) {
        if (rolloutId == null) {
            return 0L;
        }

        return rolloutGroupManagement.countByRollout(rolloutId);
    }
}
