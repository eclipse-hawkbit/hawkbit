/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.providers;

import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.ui.common.data.mappers.TargetTypeToProxyTargetTypeMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

/**
 * Data provider for {@link TargetType}, which dynamically loads a batch of
 * {@link TargetType} entities from backend and maps them to corresponding
 * {@link ProxyTargetType} entities.
 */
public class TargetTypeDataProvider extends AbstractProxyDataProvider<ProxyTargetType, TargetType, Void> {
    private static final long serialVersionUID = 1L;

    private final transient TargetTypeManagement targetTypeManagement;

    /**
     * Constructor for TargetTypeDataProvider
     *
     * @param targetTypeManagement
     *            TargetTypeManagement
     * @param mapper
     *            TagToProxyTypeMapper of TargetType
     */
    public TargetTypeDataProvider(final TargetTypeManagement targetTypeManagement,
                                  final TargetTypeToProxyTargetTypeMapper<TargetType> mapper) {
        super(mapper, Sort.by(Direction.ASC, "name"));

        this.targetTypeManagement = targetTypeManagement;
    }

    @Override
    protected Slice<TargetType> loadBackendEntities(final PageRequest pageRequest, final Void filter) {
        Slice<TargetType> all = targetTypeManagement.findAll(pageRequest);
        return all;
    }

    @Override
    protected long sizeInBackEnd(final PageRequest pageRequest, final Void filter) {
        return targetTypeManagement.count();
    }

}
