/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.state;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.repository.model.DistributionSetIdName;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.VaadinSessionScope;

/**
 * Target Table Filters.
 */
@VaadinSessionScope
@SpringComponent
public class TargetTableFilters implements Serializable {

    private static final long serialVersionUID = -5251492630546463593L;

    private final List<String> clickedTargetTags = new ArrayList<>();
    private final List<TargetUpdateStatus> clickedStatusTargetTags = new ArrayList<>();
    private String searchText;
    private DistributionSetIdName distributionSet;
    private Long pinnedDistId;
    private Long targetsCreatedCount;
    private Float progressBarCurrentValue;

    private final TargetBulkUpload bulkUpload = new TargetBulkUpload();

    /**
     * Custom target filter query use dto filter target table.
     */
    private TargetFilterQuery targetFilterQuery;
    /**
     * Flag for NO TAG button status.
     */
    private Boolean noTagSelected = Boolean.FALSE;

    /**
     * @return the bulkUpload
     */
    public TargetBulkUpload getBulkUpload() {
        return bulkUpload;
    }

    /**
     * @return the progressBarCurrentValue
     */
    public Optional<Float> getProgressBarCurrentValue() {
        return progressBarCurrentValue != null ? Optional.of(progressBarCurrentValue) : Optional.empty();
    }

    /**
     * @param progressBarCurrentValue
     *            the progressBarCurrentValue to set
     */
    public void setProgressBarCurrentValue(final Float progressBarCurrentValue) {
        this.progressBarCurrentValue = progressBarCurrentValue;
    }

    /**
     * @return the targetsCreatedCount
     */
    public Optional<Long> getTargetsCreatedCount() {
        return targetsCreatedCount != null ? Optional.of(targetsCreatedCount) : Optional.empty();
    }

    /**
     * @param targetsCreatedCount
     *            the targetsCreatedCount to set
     */
    public void setTargetsCreatedCount(final Long targetsCreatedCount) {
        this.targetsCreatedCount = targetsCreatedCount;
    }

    public void setNoTagSelected(final Boolean noTagSelected) {
        this.noTagSelected = noTagSelected;
    }

    public Boolean isNoTagSelected() {
        return noTagSelected;
    }

    public Optional<String> getSearchText() {
        return searchText == null ? Optional.empty() : Optional.of(searchText);
    }

    public Optional<DistributionSetIdName> getDistributionSet() {
        return distributionSet == null ? Optional.empty() : Optional.of(distributionSet);
    }

    public Optional<Long> getPinnedDistId() {
        return pinnedDistId == null ? Optional.empty() : Optional.of(pinnedDistId);
    }

    public void setSearchText(final String searchText) {
        this.searchText = searchText;
    }

    public void setDistributionSet(final DistributionSetIdName distributionSet) {
        this.distributionSet = distributionSet;
    }

    public void setPinnedDistId(final Long pinnedDistId) {
        this.pinnedDistId = pinnedDistId;
    }

    public List<String> getClickedTargetTags() {
        return clickedTargetTags;
    }

    public List<TargetUpdateStatus> getClickedStatusTargetTags() {
        return clickedStatusTargetTags;
    }

    /**
     * @return {@code true} if any filter is currently enabled otherwise
     *         {@code false}
     */
    public boolean hasFilter() {
        return isFilteredByTextOrDs() || hasTagsSelected() || isFilteredByStatusOrCustomFilter();
    }

    private boolean hasTagsSelected() {
        return noTagSelected || !clickedTargetTags.isEmpty();
    }

    private boolean isFilteredByTextOrDs() {
        return searchText != null || distributionSet != null;
    }

    private boolean isFilteredByStatusOrCustomFilter() {
        return !clickedStatusTargetTags.isEmpty() || targetFilterQuery != null;
    }

    /**
     * @return the targetFilterQuery
     */
    public Optional<TargetFilterQuery> getTargetFilterQuery() {
        return targetFilterQuery == null ? Optional.empty() : Optional.of(targetFilterQuery);
    }

    /**
     * @param targetFilterQuery
     *            the targetFilterQuery to set
     */
    public void setTargetFilterQuery(final TargetFilterQuery targetFilterQuery) {
        this.targetFilterQuery = targetFilterQuery;
    }

}
