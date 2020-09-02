/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.ui.common.grid.support;

import java.util.EnumMap;
import java.util.function.BiConsumer;

import org.eclipse.hawkbit.ui.common.event.FilterType;

import com.vaadin.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.data.provider.DataProvider;

/**
 * Support for Filter in Grid
 *
 * @param <T>
 *            Generic type
 * @param <F>
 *            Generic type
 */
public class FilterSupport<T, F> {
    private final ConfigurableFilterDataProvider<T, Void, F> filterDataProvider;
    private final EnumMap<FilterType, FilterTypeSetter<?>> filterTypeToSetterMapping;
    private final Runnable afterRefreshFilterCallback;

    private F entityFilter;

    /**
     * Constructor for FilterSupport
     *
     * @param dataProvider
     *            Data provider for filter
     */
    public FilterSupport(final DataProvider<T, F> dataProvider) {
        this(dataProvider, null);
    }

    /**
     * Constructor for FilterSupport
     *
     * @param dataProvider
     *            Data provider for filter
     * @param afterRefreshFilterCallback
     *            Runnable
     */
    public FilterSupport(final DataProvider<T, F> dataProvider, final Runnable afterRefreshFilterCallback) {
        this.filterDataProvider = dataProvider.withConfigurableFilter();
        this.filterTypeToSetterMapping = new EnumMap<>(FilterType.class);
        this.afterRefreshFilterCallback = afterRefreshFilterCallback;
    }

    /**
     * Add setter mapping to filter type
     *
     * @param filterType
     *            Filter type
     * @param setter
     *            Filter type setter
     * @param <R>
     *            Generic
     */
    public <R> void addMapping(final FilterType filterType, final BiConsumer<F, R> setter) {
        filterTypeToSetterMapping.put(filterType, new FilterTypeSetter<>(setter));
    }

    /**
     * Add setter mapping to filter type
     *
     * @param filterType
     *            Filter type
     * @param setter
     *            Filter type setter
     * @param defaultValue
     *            Default value for setter
     * @param <R>
     *            Generic
     */
    public <R> void addMapping(final FilterType filterType, final BiConsumer<F, R> setter, final R defaultValue) {
        filterTypeToSetterMapping.put(filterType, new FilterTypeSetter<>(setter, defaultValue));
    }

    /**
     * Update filter value
     *
     * @param filterType
     *            Filter type
     * @param filterValue
     *            Filter value
     * @param <R>
     *            Generic
     */
    public <R> void updateFilter(final FilterType filterType, final R filterValue) {
        updateFilter((BiConsumer<F, R>) filterTypeToSetterMapping.get(filterType).getSetter(), filterValue);
    }

    /**
     * Update filter value
     *
     * @param setter
     *            Filter type setter
     * @param filterValue
     *            Filter value
     * @param <R>
     *            Generic
     */
    public <R> void updateFilter(final BiConsumer<F, R> setter, final R filterValue) {
        if (setter != null) {
            setter.accept(entityFilter, filterValue);
            refreshFilter();
        }
    }

    /**
     * Refresh filter data
     */
    public void refreshFilter() {
        filterDataProvider.setFilter(entityFilter);

        if (afterRefreshFilterCallback != null) {
            afterRefreshFilterCallback.run();
        }
    }

    /**
     * @return Filter data provider
     */
    public ConfigurableFilterDataProvider<T, Void, F> getFilterDataProvider() {
        return filterDataProvider;
    }

    /**
     * Verifies if filter type is supported
     *
     * @param filterType
     *            Filter type
     *
     * @return True if filter type exist in the filter to setter map else false
     */
    public boolean isFilterTypeSupported(final FilterType filterType) {
        return filterTypeToSetterMapping.keySet().contains(filterType);
    }

    /**
     * @return Entity filter
     */
    public F getFilter() {
        return entityFilter;
    }

    /**
     * Sets the entity filter
     *
     * @param entityFilter
     *            Generic type entity filter
     */
    public void setFilter(final F entityFilter) {
        this.entityFilter = entityFilter;
    }

    /**
     * Update the filter value to default in filter to setter mapping
     */
    public void restoreFilter() {
        filterTypeToSetterMapping.values().forEach(FilterTypeSetter::restoreDefaultValue);
        refreshFilter();
    }

    private class FilterTypeSetter<R> {
        private final BiConsumer<F, R> setter;
        private final R defaultValue;

        /**
         * Constructor for FilterTypeSetter
         *
         * @param setter
         *            Setter
         */
        public FilterTypeSetter(final BiConsumer<F, R> setter) {
            this(setter, null);
        }

        /**
         * Constructor for FilterTypeSetter
         *
         * @param setter
         *            Setter
         * @param defaultValue
         *            Filter default value
         */
        public FilterTypeSetter(final BiConsumer<F, R> setter, final R defaultValue) {
            this.setter = setter;
            this.defaultValue = defaultValue;
        }

        /**
         * @return Setter
         */
        public BiConsumer<F, R> getSetter() {
            return setter;
        }

        /**
         * Reset the filter value to default
         */
        public void restoreDefaultValue() {
            if (defaultValue != null) {
                setter.accept(entityFilter, defaultValue);
            }
        }
    }
}
