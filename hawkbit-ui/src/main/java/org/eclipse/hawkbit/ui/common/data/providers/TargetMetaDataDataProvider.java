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

import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.TargetMetadata;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMetaData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * Data provider for {@link TargetMetadata}, which dynamically loads a batch of
 * {@link TargetMetadata} entities from backend and maps them to corresponding
 * {@link ProxyMetaData} entities.
 */
public class TargetMetaDataDataProvider extends AbstractMetaDataDataProvider<TargetMetadata, String> {
    private static final long serialVersionUID = 1L;

    private final transient TargetManagement targetManagement;

    /**
     * Constructor for TargetManagement
     *
     * @param targetManagement
     *            TargetManagement
     */
    public TargetMetaDataDataProvider(final TargetManagement targetManagement) {
        super();

        this.targetManagement = targetManagement;
    }

    @Override
    protected Page<TargetMetadata> loadBackendEntities(final PageRequest pageRequest,
            final String currentlySelectedControllerId) {
        if (currentlySelectedControllerId == null) {
            return Page.empty(pageRequest);
        }

        return targetManagement.findMetaDataByControllerId(pageRequest, currentlySelectedControllerId);
    }

    @Override
    protected long sizeInBackEnd(final PageRequest pageRequest, final String currentlySelectedControllerId) {
        if (currentlySelectedControllerId == null) {
            return 0L;
        }

        return targetManagement.countMetaDataByControllerId(currentlySelectedControllerId);
    }
}
