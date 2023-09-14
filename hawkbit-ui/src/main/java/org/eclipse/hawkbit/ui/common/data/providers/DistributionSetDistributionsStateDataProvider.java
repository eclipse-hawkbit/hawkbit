/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.data.providers;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter.DistributionSetFilterBuilder;
import org.eclipse.hawkbit.ui.common.data.filters.DsDistributionsFilterParams;
import org.eclipse.hawkbit.ui.common.data.mappers.DistributionSetToProxyDistributionMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

/**
 * Data provider for {@link DistributionSet}, which dynamically loads a batch of
 * {@link DistributionSet} entities from backend and maps them to corresponding
 * {@link ProxyDistributionSet} entities.
 */
public class DistributionSetDistributionsStateDataProvider
        extends AbstractProxyDataProvider<ProxyDistributionSet, DistributionSet, DsDistributionsFilterParams> {

    private static final long serialVersionUID = 1L;

    private final transient DistributionSetManagement distributionSetManagement;

    /**
     * Constructor for DistributionSetDistributionsStateDataProvider
     *
     * @param distributionSetManagement
     *            DistributionSetManagement
     * @param entityMapper
     *            DistributionSetToProxyDistributionMapper
     */
    public DistributionSetDistributionsStateDataProvider(final DistributionSetManagement distributionSetManagement,
            final DistributionSetToProxyDistributionMapper entityMapper) {
        super(entityMapper);

        this.distributionSetManagement = distributionSetManagement;
    }

    @Override
    protected Slice<DistributionSet> loadBackendEntities(final PageRequest pageRequest,
            final DsDistributionsFilterParams filter) {
        return distributionSetManagement.findByDistributionSetFilter(pageRequest, buildDsFilter(filter));
    }

    private DistributionSetFilter buildDsFilter(final DsDistributionsFilterParams filter) {
        final DistributionSetFilterBuilder builder = new DistributionSetFilterBuilder().setIsDeleted(false);

        if (filter != null) {
            builder.setSearchText(filter.getSearchText()).setSelectDSWithNoTag(false).setTypeId(filter.getDsTypeId());
        }

        return builder.build();
    }

    @Override
    protected long sizeInBackEnd(final PageRequest pageRequest, final DsDistributionsFilterParams filter) {
        return distributionSetManagement.countByDistributionSetFilter(buildDsFilter(filter));
    }
}
