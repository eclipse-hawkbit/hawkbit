/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.providers;

import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.ui.common.data.mappers.TargetFilterQueryToProxyTargetFilterMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetFilterQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

/**
 * Data provider for {@link TargetFilterQuery}, which dynamically loads a batch
 * of {@link TargetFilterQuery} entities from backend and maps them to
 * corresponding {@link ProxyTargetFilterQuery} entities.
 */
public class TargetFilterQueryDetailsDataProvider
        extends AbstractProxyDataProvider<ProxyTargetFilterQuery, TargetFilterQuery, Long> {
    private static final long serialVersionUID = 1L;

    private final transient TargetFilterQueryManagement targetFilterQueryManagement;

    /**
     * Constructor for TargetFilterQueryDetailsDataProvider
     *
     * @param targetFilterQueryManagement
     *          TargetFilterQueryManagement
     * @param entityMapper
     *          TargetFilterQueryToProxyTargetFilterMapper
     */
    public TargetFilterQueryDetailsDataProvider(final TargetFilterQueryManagement targetFilterQueryManagement,
            final TargetFilterQueryToProxyTargetFilterMapper entityMapper) {
        super(entityMapper, Sort.by(Direction.ASC, "name"));

        this.targetFilterQueryManagement = targetFilterQueryManagement;
    }

    @Override
    protected Page<TargetFilterQuery> loadBackendEntities(final PageRequest pageRequest, final Long dsId) {
        if (dsId == null) {
            return Page.empty(pageRequest);
        }

        return targetFilterQueryManagement.findByAutoAssignDSAndRsql(pageRequest, dsId, null);
    }

    @Override
    protected long sizeInBackEnd(final PageRequest pageRequest, final Long dsId) {
        if (dsId == null) {
            return 0L;
        }

        return loadBackendEntities(pageRequest, dsId).getTotalElements();
    }
}
