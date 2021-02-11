/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.providers;

import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.common.data.mappers.TagToProxyTagMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

/**
 * Data provider for {@link TargetTag}, which dynamically loads a batch of
 * {@link TargetTag} entities from backend and maps them to corresponding
 * {@link ProxyTag} entities.
 */
public class TargetTagDataProvider extends AbstractProxyDataProvider<ProxyTag, TargetTag, Void> {
    private static final long serialVersionUID = 1L;

    private final transient TargetTagManagement tagManagementService;

    /**
     * Constructor for TargetTagDataProvider
     *
     * @param tagManagementService
     *          TargetTagManagement
     * @param mapper
     *          TagToProxyTagMapper of TargetTag
     */
    public TargetTagDataProvider(final TargetTagManagement tagManagementService,
            final TagToProxyTagMapper<TargetTag> mapper) {
        super(mapper, Sort.by(Direction.ASC, "name"));

        this.tagManagementService = tagManagementService;
    }

    @Override
    protected Page<TargetTag> loadBackendEntities(final PageRequest pageRequest, final Void filter) {
        return tagManagementService.findAll(pageRequest);
    }

    @Override
    protected long sizeInBackEnd(final PageRequest pageRequest, final Void filter) {
        return loadBackendEntities(pageRequest, filter).getTotalElements();
    }

}
