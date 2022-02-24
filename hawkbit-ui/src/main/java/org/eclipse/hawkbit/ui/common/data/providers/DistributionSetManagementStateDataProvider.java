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
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter.DistributionSetFilterBuilder;
import org.eclipse.hawkbit.ui.common.data.filters.DsManagementFilterParams;
import org.eclipse.hawkbit.ui.common.data.mappers.DistributionSetToProxyDistributionMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.util.StringUtils;

/**
 * Data provider for {@link DistributionSet}, which dynamically loads a batch of
 * {@link DistributionSet} entities from backend and maps them to corresponding
 * {@link ProxyDistributionSet} entities.
 */
public class DistributionSetManagementStateDataProvider
        extends AbstractProxyDataProvider<ProxyDistributionSet, DistributionSet, DsManagementFilterParams> {

    private static final long serialVersionUID = 1L;

    private final transient DistributionSetManagement distributionSetManagement;

    /**
     * Constructor for DistributionSetManagementStateDataProvider
     *
     * @param distributionSetManagement
     *            DistributionSetManagement
     * @param entityMapper
     *            DistributionSetToProxyDistributionMapper
     */
    public DistributionSetManagementStateDataProvider(final DistributionSetManagement distributionSetManagement,
            final DistributionSetToProxyDistributionMapper entityMapper) {
        super(entityMapper);

        this.distributionSetManagement = distributionSetManagement;
    }

    @Override
    protected Slice<DistributionSet> loadBackendEntities(final PageRequest pageRequest,
            final DsManagementFilterParams filter) {
        if (filter == null) {
            return distributionSetManagement.findByCompleted(pageRequest, true);
        }

        final String pinnedControllerId = filter.getPinnedTargetControllerId();
        if (!StringUtils.isEmpty(pinnedControllerId)) {
            return distributionSetManagement.findByDistributionSetFilterOrderByLinkedTarget(pageRequest,
                    buildDsFilter(filter), pinnedControllerId);
        }

        return distributionSetManagement.findByDistributionSetFilter(pageRequest, buildDsFilter(filter));
    }

    private DistributionSetFilter buildDsFilter(final DsManagementFilterParams filter) {
        return new DistributionSetFilterBuilder().setIsDeleted(false).setIsComplete(true)
                .setSearchText(filter.getSearchText()).setSelectDSWithNoTag(filter.isNoTagClicked())
                .setTagNames(filter.getDistributionSetTags()).build();
    }

    @Override
    protected long sizeInBackEnd(final PageRequest pageRequest, final DsManagementFilterParams filter) {
        if (filter == null) {
            return distributionSetManagement.countByCompleted(true);
        }

        return distributionSetManagement.countByDistributionSetFilter(buildDsFilter(filter));
    }

}
