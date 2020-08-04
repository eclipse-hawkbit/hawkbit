/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.providers;

import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

import com.vaadin.data.provider.AbstractBackEndDataProvider;
import com.vaadin.data.provider.Query;

/**
 * Base class for loading a batch of entities from backend mapping them to UI
 * {@link ProxyIdentifiableEntity} entities.
 *
 * @param <T>
 *          Generic type of ProxyIdentifiableEntity
 * @param <U>
 *          Generic type
 * @param <F>
 *          Generic type
 */
public abstract class AbstractGenericDataProvider<T extends ProxyIdentifiableEntity, U, F>
        extends AbstractBackEndDataProvider<T, F> {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractGenericDataProvider.class);

    private final Sort defaultSortOrder;

    /**
     * Constructor for GenericDataProvider
     *
     * @param defaultSortOrder
     *          Sort
     */
    public AbstractGenericDataProvider(final Sort defaultSortOrder) {
        this.defaultSortOrder = defaultSortOrder;
    }

    @Override
    protected Stream<T> fetchFromBackEnd(final Query<T, F> query) {
        return getProxyEntities(
                loadBackendEntities(convertToPageRequest(query, defaultSortOrder), query.getFilter().orElse(null)));
    }

    private PageRequest convertToPageRequest(final Query<T, F> query, final Sort sort) {
        return new OffsetBasedPageRequest(query.getOffset(), query.getLimit(), sort);
    }

    protected abstract Slice<U> loadBackendEntities(final PageRequest pageRequest, F filter);

    protected abstract Stream<T> getProxyEntities(final Slice<U> backendEntities);

    @Override
    protected int sizeInBackEnd(final Query<T, F> query) {
        final long size = sizeInBackEnd(convertToPageRequest(query, defaultSortOrder), query.getFilter().orElse(null));

        try {
            return Math.toIntExact(size);
        } catch (final ArithmeticException e) {
            LOG.trace("Error converting size in backend from UI Dataprovider: {}", e.getMessage());

            return Integer.MAX_VALUE;
        }
    }

    protected abstract long sizeInBackEnd(final PageRequest pageRequest, F filter);

    @Override
    public Object getId(final T item) {
        Objects.requireNonNull(item, "Cannot provide an id for a null item.");
        return item.getId();
    }
}
