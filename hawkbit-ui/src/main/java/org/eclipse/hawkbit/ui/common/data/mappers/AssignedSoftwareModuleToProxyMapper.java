/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.data.mappers;

import org.eclipse.hawkbit.repository.model.AssignedSoftwareModule;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;

/**
 * Maps {@link AssignedSoftwareModule} entities, fetched from backend, to the
 * {@link ProxySoftwareModule} entities.
 */
public class AssignedSoftwareModuleToProxyMapper
        implements IdentifiableEntityToProxyIdentifiableEntityMapper<ProxySoftwareModule, AssignedSoftwareModule> {

    private final SoftwareModuleToProxyMapper softwareModuleToProxyMapper;

    /**
     * Constructor for SoftwareModuleToProxyMapper
     *
     * @param softwareModuleToProxyMapper
     *          SoftwareModuleToProxyMapper
     */
    public AssignedSoftwareModuleToProxyMapper(final SoftwareModuleToProxyMapper softwareModuleToProxyMapper) {
        this.softwareModuleToProxyMapper = softwareModuleToProxyMapper;
    }

    @Override
    public ProxySoftwareModule map(final AssignedSoftwareModule assignedSwModule) {
        final ProxySoftwareModule proxyModule = softwareModuleToProxyMapper.map(assignedSwModule.getSoftwareModule());
        proxyModule.setAssigned(assignedSwModule.isAssigned());

        return proxyModule;
    }
}
