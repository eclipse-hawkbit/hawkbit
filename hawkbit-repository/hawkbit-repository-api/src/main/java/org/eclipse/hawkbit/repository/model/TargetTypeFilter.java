/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

/**
 * Holds target type filter parameters.
 */
public final class TargetTypeFilter {
    /**
     * Target type filter builder.
     */
    public static class TargetTypeFilterBuilder {
        private String searchText;
        private String filterString;

        /**
         * Build filter.
         *
         * @return TargetTypeFilter
         */
        public TargetTypeFilter build() {
            return new TargetTypeFilter(this);
        }

        public TargetTypeFilterBuilder setSearchText(final String searchText) {
            this.searchText = searchText;
            return this;
        }

        public TargetTypeFilterBuilder setFilterString(final String filterString) {
            this.filterString = filterString;
            return this;
        }

    }

    private final String searchText;
    private final String filterString;

    /**
     * Parametric constructor.
     *
     * @param builder
     *            TargetTypeFilterBuilder
     */
    public TargetTypeFilter(final TargetTypeFilterBuilder builder) {
        this.searchText = builder.searchText;
        this.filterString = builder.filterString;
    }

    public String getSearchText() {
        return searchText;
    }

    public String getFilterString() {
        return filterString;
    }

}
