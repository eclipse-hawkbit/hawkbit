/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.Collection;

/**
 * Holds distribution set filter parameters.
 */
public final class DistributionSetFilter {
    /**
     * Distribution set filter builder.
     */
    public static class DistributionSetFilterBuilder {
        private Boolean isDeleted;
        private Boolean isComplete;
        private DistributionSetType type;
        private String searchText;
        private Boolean selectDSWithNoTag;
        private Collection<String> tagNames;
        private String assignedTargetId;
        private String installedTargetId;

        /**
         * Build filter.
         *
         * @return DistributionSetFilter
         */
        public DistributionSetFilter build() {
            return new DistributionSetFilter(this);
        }

        public DistributionSetFilterBuilder setAssignedTargetId(final String assignedTargetId) {
            this.assignedTargetId = assignedTargetId;
            return this;
        }

        public DistributionSetFilterBuilder setInstalledTargetId(final String installedTargetId) {
            this.installedTargetId = installedTargetId;
            return this;
        }

        public DistributionSetFilterBuilder setIsComplete(final Boolean isComplete) {
            this.isComplete = isComplete;
            return this;
        }

        public DistributionSetFilterBuilder setIsDeleted(final Boolean isDeleted) {
            this.isDeleted = isDeleted;
            return this;
        }

        public DistributionSetFilterBuilder setSearchText(final String searchText) {
            this.searchText = searchText;
            return this;
        }

        public DistributionSetFilterBuilder setSelectDSWithNoTag(final Boolean selectDSWithNoTag) {
            this.selectDSWithNoTag = selectDSWithNoTag;
            return this;
        }

        public DistributionSetFilterBuilder setTagNames(final Collection<String> tagNames) {
            this.tagNames = tagNames;
            return this;
        }

        public DistributionSetFilterBuilder setType(final DistributionSetType type) {
            this.type = type;
            return this;
        }

    }

    private final Boolean isDeleted;
    private final Boolean isComplete;
    private final DistributionSetType type;
    private final String searchText;
    private final Boolean selectDSWithNoTag;
    private final Collection<String> tagNames;
    private final String assignedTargetId;

    private final String installedTargetId;

    /**
     * Parametric constructor.
     *
     * @param builder
     *            DistributionSetFilterBuilder
     */
    public DistributionSetFilter(final DistributionSetFilterBuilder builder) {
        this.isDeleted = builder.isDeleted;
        this.isComplete = builder.isComplete;
        this.type = builder.type;
        this.searchText = builder.searchText;
        this.selectDSWithNoTag = builder.selectDSWithNoTag;
        this.tagNames = builder.tagNames;
        this.assignedTargetId = builder.assignedTargetId;
        this.installedTargetId = builder.installedTargetId;
    }

    public String getAssignedTargetId() {
        return assignedTargetId;
    }

    public String getInstalledTargetId() {
        return installedTargetId;
    }

    public Boolean getIsComplete() {
        return isComplete;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public String getSearchText() {
        return searchText;
    }

    public Boolean getSelectDSWithNoTag() {
        return selectDSWithNoTag;
    }

    public Collection<String> getTagNames() {
        return tagNames;
    }

    public DistributionSetType getType() {
        return type;
    }

}
