/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.providers;

import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.common.data.mappers.IdentifiableEntityToProxyIdentifiableEntityMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

/**
 * Data provider for {@link SoftwareModuleType}, which dynamically loads a batch
 * of {@link SoftwareModuleType} entities from backend and maps them to
 * corresponding output type.
 * 
 * @param <T>
 *            output type
 */
public class SoftwareModuleTypeDataProvider<T extends ProxyIdentifiableEntity>
        extends AbstractProxyDataProvider<T, SoftwareModuleType, String> {

    private static final long serialVersionUID = 1L;

    private final transient SoftwareModuleTypeManagement softwareModuleTypeManagement;

    /**
     * Constructor for SoftwareModuleTypeDataProvider
     *
     * @param softwareModuleTypeManagement
     *            SoftwareModuleTypeManagement
     * @param mapper
     *            TypeToProxyTypeMapper of softwareModuleType
     */
    public SoftwareModuleTypeDataProvider(final SoftwareModuleTypeManagement softwareModuleTypeManagement,
            final IdentifiableEntityToProxyIdentifiableEntityMapper<T, SoftwareModuleType> mapper) {
        super(mapper, Sort.by(Direction.ASC, "name"));
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
    }

    @Override
    protected Slice<SoftwareModuleType> loadBackendEntities(final PageRequest pageRequest, final String filter) {
        return softwareModuleTypeManagement.findAll(pageRequest);
    }

    @Override
    protected long sizeInBackEnd(final PageRequest pageRequest, final String filter) {
        return softwareModuleTypeManagement.count();
    }

}
