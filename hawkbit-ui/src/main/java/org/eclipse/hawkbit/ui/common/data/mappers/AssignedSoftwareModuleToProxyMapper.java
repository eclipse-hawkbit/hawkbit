/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
