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
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter.DistributionSetFilterBuilder;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.ui.common.data.filters.DsDistributionsFilterParams;
import org.eclipse.hawkbit.ui.common.data.mappers.DistributionSetToProxyDistributionMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * Data provider for {@link DistributionSet}, which dynamically loads a batch of
 * {@link DistributionSet} entities from backend and maps them to corresponding
 * {@link ProxyDistributionSet} entities.
 */
public class DistributionSetDistributionsStateDataProvider
        extends AbstractProxyDataProvider<ProxyDistributionSet, DistributionSet, DsDistributionsFilterParams> {

    private static final long serialVersionUID = 1L;

    private final transient DistributionSetManagement distributionSetManagement;
    private final transient DistributionSetTypeManagement distributionSetTypeManagement;

    /**
     * Constructor for DistributionSetDistributionsStateDataProvider
     *
     * @param distributionSetManagement
     *            DistributionSetManagement
     * @param distributionSetTypeManagement
     *            DistributionSetTypeManagement
     * @param entityMapper
     *            DistributionSetToProxyDistributionMapper
     */
    public DistributionSetDistributionsStateDataProvider(final DistributionSetManagement distributionSetManagement,
            final DistributionSetTypeManagement distributionSetTypeManagement,
            final DistributionSetToProxyDistributionMapper entityMapper) {
        super(entityMapper);

        this.distributionSetManagement = distributionSetManagement;
        this.distributionSetTypeManagement = distributionSetTypeManagement;
    }

    @Override
    protected Page<DistributionSet> loadBackendEntities(final PageRequest pageRequest,
            final DsDistributionsFilterParams filter) {
        final DistributionSetFilterBuilder builder = new DistributionSetFilterBuilder().setIsDeleted(false);

        if (filter != null) {
            final DistributionSetType type = filter.getDsTypeId() == null ? null
                    : distributionSetTypeManagement.get(filter.getDsTypeId()).orElse(null);

            builder.setSearchText(filter.getSearchText()).setSelectDSWithNoTag(false).setType(type);
        }

        return distributionSetManagement.findByDistributionSetFilter(pageRequest, builder.build());
    }

    @Override
    protected long sizeInBackEnd(final PageRequest pageRequest, final DsDistributionsFilterParams filter) {
        return loadBackendEntities(PageRequest.of(0, 1), filter).getTotalElements();
    }
}
