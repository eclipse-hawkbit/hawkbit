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
import org.eclipse.hawkbit.ui.common.data.mappers.IdentifiableEntityToProxyIdentifiableEntityMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.util.StringUtils;

/**
 * Data provider for {@link TargetTypeManagement}, which dynamically loads a
 * batch of {@link TargetType} entities from backend and maps them to
 * corresponding output type.
 * 
 * @param <T>
 *            output type
 */
public class TargetTypeDataProvider<T extends ProxyIdentifiableEntity>
        extends AbstractProxyDataProvider<T, TargetType, String> {
    private static final long serialVersionUID = 1L;
    private final transient TargetTypeManagement targetTypeManagement;

    public TargetTypeDataProvider(final TargetTypeManagement targetTypeManagement, IdentifiableEntityToProxyIdentifiableEntityMapper<T, TargetType> mapper) {
        super(mapper, Sort.by(Direction.ASC, "name"));
        this.targetTypeManagement = targetTypeManagement;

    }

    @Override
    protected Page<TargetType> loadBackendEntities(PageRequest pageRequest, String filter) {
        if (!StringUtils.isEmpty(filter)){
            return targetTypeManagement.findByName(pageRequest, filter);
        }
        return targetTypeManagement.findAll(pageRequest);
    }

    @Override
    protected long sizeInBackEnd(PageRequest pageRequest, String filter) {
        return loadBackendEntities(PageRequest.of(0, 1), filter).getTotalElements();
    }
}
