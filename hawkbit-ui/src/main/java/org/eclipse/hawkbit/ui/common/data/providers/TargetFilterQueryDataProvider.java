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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.util.StringUtils;

/**
 * Data provider for {@link TargetFilterQuery}, which dynamically loads a batch
 * of {@link TargetFilterQuery} entities from backend and maps them to
 * corresponding {@link ProxyTargetFilterQuery} entities.
 */
public class TargetFilterQueryDataProvider
        extends AbstractProxyDataProvider<ProxyTargetFilterQuery, TargetFilterQuery, String> {
    private static final long serialVersionUID = 1L;

    private final transient TargetFilterQueryManagement targetFilterQueryManagement;

    /**
     * Constructor for TargetFilterQueryDataProvider
     *
     * @param targetFilterQueryManagement
     *          TargetFilterQueryManagement
     * @param entityMapper
     *          TargetFilterQueryToProxyTargetFilterMapper
     */
    public TargetFilterQueryDataProvider(final TargetFilterQueryManagement targetFilterQueryManagement,
            final TargetFilterQueryToProxyTargetFilterMapper entityMapper) {
        super(entityMapper, Sort.by(Direction.ASC, "name"));

        this.targetFilterQueryManagement = targetFilterQueryManagement;
    }

    @Override
    protected Slice<TargetFilterQuery> loadBackendEntities(final PageRequest pageRequest, final String filter) {
        if (StringUtils.isEmpty(filter)) {
            return targetFilterQueryManagement.findAll(pageRequest);
        }

        return targetFilterQueryManagement.findByName(pageRequest, filter);
    }

    @Override
    protected long sizeInBackEnd(final PageRequest pageRequest, final String filter) {
        if (StringUtils.isEmpty(filter)) {
            return targetFilterQueryManagement.count();
        }

        return targetFilterQueryManagement.findByName(pageRequest, filter).getTotalElements();
    }
}
