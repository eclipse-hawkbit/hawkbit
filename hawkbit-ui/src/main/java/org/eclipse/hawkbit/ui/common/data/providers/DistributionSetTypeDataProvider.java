/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.providers;

import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.ui.common.data.mappers.IdentifiableEntityToProxyIdentifiableEntityMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

/**
 * Data provider for {@link DistributionSetType}, which dynamically loads a
 * batch of {@link DistributionSetType} entities from backend and maps them to
 * corresponding output type.
 * 
 * @param <T>
 *            output type
 */
public class DistributionSetTypeDataProvider<T extends ProxyIdentifiableEntity>
        extends AbstractProxyDataProvider<T, DistributionSetType, String> {
    private static final long serialVersionUID = 1L;

    private final transient DistributionSetTypeManagement distributionSetTypeManagement;

    /**
     * Constructor for DistributionSetTypeDataProvider
     *
     * @param distributionSetTypeManagement
     *            DistributionSetTypeManagement
     * @param mapper
     *            TypeToProxyTypeMapper of DistributionSetType
     */
    public DistributionSetTypeDataProvider(final DistributionSetTypeManagement distributionSetTypeManagement,
            final IdentifiableEntityToProxyIdentifiableEntityMapper<T, DistributionSetType> mapper) {
        super(mapper, Sort.by(Direction.ASC, "name"));

        this.distributionSetTypeManagement = distributionSetTypeManagement;
    }

    @Override
    protected Slice<DistributionSetType> loadBackendEntities(final PageRequest pageRequest, final String filter) {
        return distributionSetTypeManagement.findAll(pageRequest);
    }

    @Override
    protected long sizeInBackEnd(final PageRequest pageRequest, final String filter) {
        return distributionSetTypeManagement.count();
    }

}
