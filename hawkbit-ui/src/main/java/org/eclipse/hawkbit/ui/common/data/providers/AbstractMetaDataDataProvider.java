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

import java.util.stream.Stream;

import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMetaData;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

/**
 * Abstract data provider for {@link MetaData}, which dynamically loads a batch
 * of {@link MetaData} entities from backend and maps them to corresponding
 * {@link ProxyMetaData} entities.
 *
 * @param <U>
 *          Generic type of MetaData
 * @param <F>
 *          Generic type of Proxy meta Data provider
 */
public abstract class AbstractMetaDataDataProvider<U extends MetaData, F>
        extends AbstractGenericDataProvider<ProxyMetaData, U, F> {
    private static final long serialVersionUID = 1L;

    protected AbstractMetaDataDataProvider() {
        this(Sort.by(Direction.DESC, "key"));
    }

    protected AbstractMetaDataDataProvider(final Sort defaultSortOrder) {
        super(defaultSortOrder);
    }

    @Override
    protected Stream<ProxyMetaData> getProxyEntities(final Slice<U> backendEntities) {
        return backendEntities.stream().map(this::createProxyMetaData);
    }

    protected ProxyMetaData createProxyMetaData(final U metadata) {
        final ProxyMetaData proxyMetaData = new ProxyMetaData();

        proxyMetaData.setEntityId(metadata.getEntityId());
        proxyMetaData.setKey(metadata.getKey());
        proxyMetaData.setValue(metadata.getValue());

        return proxyMetaData;
    }
}
