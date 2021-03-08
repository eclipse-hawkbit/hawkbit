/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.providers;

import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.data.mappers.TargetToProxyTargetMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.util.StringUtils;

/**
 * Data provider for {@link Target}, which dynamically loads a batch of
 * {@link Target} entities from backend and maps them to corresponding
 * {@link ProxyTarget} entities.
 */
public class TargetFilterStateDataProvider extends AbstractProxyDataProvider<ProxyTarget, Target, String> {

    private static final long serialVersionUID = 1L;

    private final transient TargetManagement targetManagement;

    /**
     * Constructor for TargetFilterStateDataProvider
     *
     * @param targetManagement
     *          TargetManagement
     * @param entityMapper
     *          TargetToProxyTargetMapper
     */
    public TargetFilterStateDataProvider(final TargetManagement targetManagement,
            final TargetToProxyTargetMapper entityMapper) {
        super(entityMapper, Sort.by(Direction.ASC, "name"));

        this.targetManagement = targetManagement;
    }

    @Override
    protected Slice<Target> loadBackendEntities(final PageRequest pageRequest, final String filter) {
        if (StringUtils.isEmpty(filter)) {
            return Page.empty(pageRequest);
        }

        return targetManagement.findByRsql(pageRequest, filter);
    }

    @Override
    protected long sizeInBackEnd(final PageRequest pageRequest, final String filter) {
        if (StringUtils.isEmpty(filter)) {
            return 0L;
        }

        return targetManagement.countByRsql(filter);
    }
}
