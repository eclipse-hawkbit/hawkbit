/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.providers;

import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.ui.common.data.mappers.RolloutToProxyRolloutMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRollout;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.util.StringUtils;

/**
 * Data provider for {@link Rollout}, which dynamically loads a batch of
 * {@link Rollout} entities from backend and maps them to corresponding
 * {@link ProxyRollout} entities.
 */
public class RolloutDataProvider extends AbstractProxyDataProvider<ProxyRollout, Rollout, String> {

    private static final long serialVersionUID = 1L;

    private final transient RolloutManagement rolloutManagement;

    /**
     * Constructor
     * 
     * @param rolloutManagement
     *            to get the entities
     * @param entityMapper
     *            entityMapper
     */
    public RolloutDataProvider(final RolloutManagement rolloutManagement,
            final RolloutToProxyRolloutMapper entityMapper) {
        super(entityMapper, Sort.by(Direction.DESC, "lastModifiedAt"));

        this.rolloutManagement = rolloutManagement;
    }

    @Override
    protected Slice<Rollout> loadBackendEntities(final PageRequest pageRequest, final String filter) {
        if (StringUtils.isEmpty(filter)) {
            return rolloutManagement.findAllWithDetailedStatus(pageRequest, false);
        }

        return rolloutManagement.findByFiltersWithDetailedStatus(pageRequest, filter, false);
    }

    @Override
    protected long sizeInBackEnd(final PageRequest pageRequest, final String filter) {
        if (StringUtils.isEmpty(filter)) {
            return rolloutManagement.count();
        }

        return rolloutManagement.countByFilters(filter);
    }
}
