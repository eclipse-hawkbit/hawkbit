/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.providers;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter.DistributionSetFilterBuilder;
import org.eclipse.hawkbit.ui.common.data.mappers.DistributionSetToProxyDistributionMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.util.StringUtils;

/**
 * Data provider for {@link DistributionSet}, which dynamically loads a batch of
 * {@link DistributionSet} entities from backend and maps them to corresponding
 * {@link ProxyDistributionSet} entities.
 */
public class DistributionSetStatelessDataProvider
        extends AbstractProxyDataProvider<ProxyDistributionSet, DistributionSet, String> {

    private static final long serialVersionUID = 1L;

    private final transient DistributionSetManagement distributionSetManagement;

    /**
     * Constructor for DistributionSetStatelessDataProvider
     *
     * @param distributionSetManagement
     *          DistributionSetManagement
     * @param entityMapper
     *          DistributionSetToProxyDistributionMapper
     */
    public DistributionSetStatelessDataProvider(final DistributionSetManagement distributionSetManagement,
            final DistributionSetToProxyDistributionMapper entityMapper) {
        super(entityMapper, Sort.by(Direction.ASC, "name", "version"));

        this.distributionSetManagement = distributionSetManagement;
    }

    @Override
    protected Page<DistributionSet> loadBackendEntities(final PageRequest pageRequest, final String filter) {
        final DistributionSetFilterBuilder builder = new DistributionSetFilterBuilder().setIsDeleted(false)
                .setIsComplete(true);

        if (!StringUtils.isEmpty(filter)) {
            builder.setFilterString(filter);
        }

        return distributionSetManagement.findByDistributionSetFilter(pageRequest, builder.build());
    }

    @Override
    protected long sizeInBackEnd(final PageRequest pageRequest, final String filter) {
        return loadBackendEntities(pageRequest, filter).getTotalElements();
    }
}
