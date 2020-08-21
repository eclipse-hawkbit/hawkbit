/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.event;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;

/**
 * payload event for filter changed
 *
 * @param <F>
 *          Generic type
 */
public class FilterChangedEventPayload<F> extends EventViewAware {
    private final Class<? extends ProxyIdentifiableEntity> entityType;
    private final FilterType filterType;
    private final F filterValue;

    /**
     * Constructor for FilterChangedEventPayload
     *
     * @param entityType
     *          Event payload of identifiable entity type
     * @param filterType
     *          FilterType
     * @param filterValue
     *          Generic type filter value
     * @param view
     *          EventView
     */
    public FilterChangedEventPayload(final Class<? extends ProxyIdentifiableEntity> entityType, final FilterType filterType,
            final F filterValue, final EventView view) {
        super(view);

        this.entityType = entityType;
        this.filterType = filterType;
        this.filterValue = filterValue;
    }

    /**
     * @return Event payload of identifiable entity type
     */
    public Class<? extends ProxyIdentifiableEntity> getEntityType() {
        return entityType;
    }

    /**
     * @return Filter type
     */
    public FilterType getFilterType() {
        return filterType;
    }

    /**
     * @return Filter value
     */
    public F getFilterValue() {
        return filterValue;
    }
}
