/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.providers;

import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.model.AssignedSoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.common.data.filters.SwFilterParams;
import org.eclipse.hawkbit.ui.common.data.mappers.AssignedSoftwareModuleToProxyMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.util.StringUtils;

/**
 * Data provider for {@link AssignedSoftwareModule}, which dynamically loads a
 * batch of {@link AssignedSoftwareModule} entities from backend and maps them
 * to corresponding {@link ProxySoftwareModule} entities.
 */
public class SoftwareModuleDataProvider
        extends AbstractProxyDataProvider<ProxySoftwareModule, AssignedSoftwareModule, SwFilterParams> {

    private static final long serialVersionUID = 1L;

    private final transient SoftwareModuleManagement softwareModuleManagement;

    /**
     * Constructor for SoftwareModuleDataProvider
     *
     * @param softwareModuleManagement
     *            SoftwareModuleManagement
     * @param entityMapper
     *            AssignedSoftwareModuleToProxyMapper
     */
    public SoftwareModuleDataProvider(final SoftwareModuleManagement softwareModuleManagement,
            final AssignedSoftwareModuleToProxyMapper entityMapper) {
        super(entityMapper, Sort.by(Direction.ASC, "name", "version"));

        this.softwareModuleManagement = softwareModuleManagement;
    }

    @Override
    protected Slice<AssignedSoftwareModule> loadBackendEntities(final PageRequest pageRequest,
            final SwFilterParams filter) {
        if (filter == null) {
            return getAllAssignedSms(pageRequest);
        }

        final String searchText = filter.getSearchText();
        final Long typeId = filter.getSoftwareModuleTypeId();
        final Long dsId = filter.getLastSelectedDistributionId();

        if (dsId != null) {
            return softwareModuleManagement.findAllOrderBySetAssignmentAndModuleNameAscModuleVersionAsc(pageRequest,
                    dsId, searchText, typeId);
        }

        if (!StringUtils.isEmpty(searchText) || typeId != null) {
            return mapToAssignedSoftwareModule(
                    softwareModuleManagement.findByTextAndType(pageRequest, searchText, typeId));
        }

        return getAllAssignedSms(pageRequest);
    }

    private Slice<AssignedSoftwareModule> getAllAssignedSms(final PageRequest pageRequest) {
        return mapToAssignedSoftwareModule(softwareModuleManagement.findAll(pageRequest));
    }

    private static Slice<AssignedSoftwareModule> mapToAssignedSoftwareModule(final Slice<SoftwareModule> smSlice) {
        return smSlice.map(sm -> new AssignedSoftwareModule(sm, false));
    }

    @Override
    protected long sizeInBackEnd(final PageRequest pageRequest, final SwFilterParams filter) {
        if (filter == null) {
            return softwareModuleManagement.count();
        }

        final String searchText = filter.getSearchText();
        final Long typeId = filter.getSoftwareModuleTypeId();

        if (!StringUtils.isEmpty(searchText) || typeId != null) {
            return softwareModuleManagement.countByTextAndType(searchText, typeId);
        }

        return softwareModuleManagement.count();
    }
}
