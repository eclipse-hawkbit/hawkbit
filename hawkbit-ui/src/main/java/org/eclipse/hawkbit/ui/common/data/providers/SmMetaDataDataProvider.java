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

import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMetaData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * Data provider for {@link SoftwareModuleMetadata}, which dynamically loads a
 * batch of {@link SoftwareModuleMetadata} entities from backend and maps them
 * to corresponding {@link ProxyMetaData} entities.
 */
public class SmMetaDataDataProvider extends AbstractMetaDataDataProvider<SoftwareModuleMetadata, Long> {
    private static final long serialVersionUID = 1L;

    private final transient SoftwareModuleManagement softwareModuleManagement;

    /**
     * Constructor for SmMetaDataDataProvider
     *
     * @param softwareModuleManagement
     *            SoftwareModuleManagement
     */
    public SmMetaDataDataProvider(final SoftwareModuleManagement softwareModuleManagement) {
        super();

        this.softwareModuleManagement = softwareModuleManagement;
    }

    @Override
    protected ProxyMetaData createProxyMetaData(final SoftwareModuleMetadata smMetadata) {
        final ProxyMetaData proxyMetaData = super.createProxyMetaData(smMetadata);
        proxyMetaData.setVisibleForTargets(smMetadata.isTargetVisible());

        return proxyMetaData;
    }

    @Override
    protected Page<SoftwareModuleMetadata> loadBackendEntities(final PageRequest pageRequest, final Long smId) {
        if (smId == null) {
            return Page.empty(pageRequest);
        }

        return softwareModuleManagement.findMetaDataBySoftwareModuleId(pageRequest, smId);
    }

    @Override
    protected long sizeInBackEnd(final PageRequest pageRequest, final Long smId) {
        if (smId == null) {
            return 0L;
        }

        return softwareModuleManagement.countMetaDataBySoftwareModuleId(smId);
    }
}
