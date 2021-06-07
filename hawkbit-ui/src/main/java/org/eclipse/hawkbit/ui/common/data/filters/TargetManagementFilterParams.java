/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.filters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.ui.common.data.providers.TargetManagementStateDataProvider;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.google.common.base.MoreObjects;

/**
 * Filter params for {@link TargetManagementStateDataProvider}.
 */
public class TargetManagementFilterParams implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long pinnedDistId;
    private String searchText;
    private Collection<TargetUpdateStatus> targetUpdateStatusList;
    private boolean overdueState;
    private Long distributionId;
    private boolean noTagClicked;
    private Collection<String> targetTags;
    private Long targetFilterQueryId;

    /**
     * Constructor for TargetManagementFilterParams to initialize
     */
    public TargetManagementFilterParams() {
        this(null, null, Collections.emptyList(), false, null, false, Collections.emptyList(), null);
    }

    /**
     * Constructor.
     * 
     * @param pinnedDistId
     *            Id for Pinned distribution
     * @param searchText
     *            String as search text
     * @param targetUpdateStatusList
     *            Collection of TargetUpdateStatus
     * @param overdueState
     *            boolean
     * @param distributionId
     *            Long
     * @param noTagClicked
     *            boolean
     * @param targetTags
     *            Collection of string as target tags
     * @param targetFilterQueryId
     *            Id for target filter query
     */
    public TargetManagementFilterParams(final Long pinnedDistId, final String searchText,
            final Collection<TargetUpdateStatus> targetUpdateStatusList, final boolean overdueState,
            final Long distributionId, final boolean noTagClicked, final Collection<String> targetTags,
            final Long targetFilterQueryId) {
        this.pinnedDistId = pinnedDistId;
        this.searchText = searchText;
        this.targetUpdateStatusList = targetUpdateStatusList;
        this.overdueState = overdueState;
        this.distributionId = distributionId;
        this.noTagClicked = noTagClicked;
        this.targetTags = targetTags;
        this.targetFilterQueryId = targetFilterQueryId;
    }

    /**
     * Copy Constructor.
     *
     * @param filter
     *            A filter to be copied
     */
    public TargetManagementFilterParams(final TargetManagementFilterParams filter) {
        this.pinnedDistId = filter.getPinnedDistId();
        this.searchText = filter.getSearchText();
        this.targetUpdateStatusList = filter.getTargetUpdateStatusList() != null
                ? new ArrayList<>(filter.getTargetUpdateStatusList())
                : null;
        this.overdueState = filter.isOverdueState();
        this.distributionId = filter.getDistributionId();
        this.noTagClicked = filter.isNoTagClicked();
        this.targetTags = filter.getTargetTags() != null ? new ArrayList<>(filter.getTargetTags()) : null;
        this.targetFilterQueryId = filter.getTargetFilterQueryId();
    }

    /**
     * Gets the flag that indicates if the filter is selected
     *
     * @return boolean <code>true</code> if the filter is selected, otherwise
     *         <code>false</code>
     */
    public boolean isAnyFilterSelected() {
        return isAnyTagSelected() || isAnyStatusFilterSelected() || isSearchActive() || isAnyComplexFilterSelected();
    }

    private boolean isAnyTagSelected() {
        return !CollectionUtils.isEmpty(targetTags) || noTagClicked;
    }

    private boolean isAnyStatusFilterSelected() {
        return !CollectionUtils.isEmpty(targetUpdateStatusList) || overdueState;
    }

    private boolean isSearchActive() {
        return !StringUtils.isEmpty(searchText);
    }

    private boolean isAnyComplexFilterSelected() {
        return distributionId != null || targetFilterQueryId != null;
    }

    /**
     * Gets the pinnedDistId
     *
     * @return Id for pinned distribution
     */
    public Long getPinnedDistId() {
        return pinnedDistId;
    }

    /**
     * Sets the pinnedDistId
     *
     * @param pinnedDistId
     *            Id for pinned distribution
     */
    public void setPinnedDistId(final Long pinnedDistId) {
        this.pinnedDistId = pinnedDistId;
    }

    /**
     * Gets the searchText
     *
     * @return searchText
     */
    public String getSearchText() {
        return searchText;
    }

    /**
     * Sets the searchText
     *
     * @param searchText
     *            Text for search
     */
    public void setSearchText(final String searchText) {
        this.searchText = !StringUtils.isEmpty(searchText) ? String.format("%%%s%%", searchText) : null;
    }

    /**
     * Gets the targetUpdateStatusList
     *
     * @return Collection of targetUpdateStatus
     */
    public Collection<TargetUpdateStatus> getTargetUpdateStatusList() {
        return targetUpdateStatusList;
    }

    /**
     * Sets the targetUpdateStatusList
     *
     * @param targetUpdateStatusList
     *            Collection of targetUpdateStatus
     */
    public void setTargetUpdateStatusList(final Collection<TargetUpdateStatus> targetUpdateStatusList) {
        this.targetUpdateStatusList = targetUpdateStatusList;
    }

    /**
     * Gets the distributionId
     *
     * @return distributionId
     */
    public Long getDistributionId() {
        return distributionId;
    }

    /**
     * Sets the distributionId
     *
     * @param distributionId
     *            Distribution Id
     */
    public void setDistributionId(final Long distributionId) {
        this.distributionId = distributionId;
    }

    /**
     * Gets the targetTags
     *
     * @return targetTags
     */
    public Collection<String> getTargetTags() {
        return targetTags;
    }

    /**
     * Sets the targetTags
     *
     * @param targetTags
     *            Collection of targetTags
     */
    public void setTargetTags(final Collection<String> targetTags) {
        this.targetTags = targetTags;
    }

    /**
     * Gets the targetFilterQueryId
     *
     * @return targetFilterQueryId Id for target filter query
     */
    public Long getTargetFilterQueryId() {
        return targetFilterQueryId;
    }

    /**
     * Sets the targetFilterQueryId
     *
     * @param targetFilterQueryId
     *            Id for target filter query
     */
    public void setTargetFilterQueryId(final Long targetFilterQueryId) {
        this.targetFilterQueryId = targetFilterQueryId;
    }

    /**
     * Gets the state of overdue
     *
     * @return overdueState <code>true</code> if the state is set, otherwise
     *         <code>false</code>
     */
    public boolean isOverdueState() {
        return overdueState;
    }

    /**
     * Sets the state of overDue
     *
     * @param overdueState
     *            <code>true</code> if the state is set, otherwise
     *            <code>false</code>
     */
    public void setOverdueState(final boolean overdueState) {
        this.overdueState = overdueState;
    }

    /**
     * Gets the status of tag clicked
     *
     * @return noTagClicked <code>true</code> if the tag is clicked, otherwise
     *         <code>false</code>
     */
    public boolean isNoTagClicked() {
        return noTagClicked;
    }

    /**
     * Sets the state of overDue
     *
     * @param noTagClicked
     *            <code>true</code> if the tag is clicked, otherwise
     *            <code>false</code>
     */
    public void setNoTagClicked(final boolean noTagClicked) {
        this.noTagClicked = noTagClicked;
    }

    // equals requires all fields in condition
    @SuppressWarnings("squid:S1067")
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TargetManagementFilterParams other = (TargetManagementFilterParams) obj;
        return Objects.equals(this.getPinnedDistId(), other.getPinnedDistId())
                && Objects.equals(this.getSearchText(), other.getSearchText())
                && Objects.equals(this.getTargetUpdateStatusList(), other.getTargetUpdateStatusList())
                && Objects.equals(this.isOverdueState(), other.isOverdueState())
                && Objects.equals(this.getDistributionId(), other.getDistributionId())
                && Objects.equals(this.isNoTagClicked(), other.isNoTagClicked())
                && Objects.equals(this.getTargetTags(), other.getTargetTags())
                && Objects.equals(this.getTargetFilterQueryId(), other.getTargetFilterQueryId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPinnedDistId(), getSearchText(), getTargetUpdateStatusList(), isOverdueState(),
                getDistributionId(), isNoTagClicked(), getTargetTags(), getTargetFilterQueryId());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("pinnedDistId", getPinnedDistId())
                .add("searchText", getSearchText()).add("targetUpdateStatusList", getTargetUpdateStatusList())
                .add("overdueState", isOverdueState()).add("distributionId", getDistributionId())
                .add("noTagClicked", isNoTagClicked()).add("targetTags", getTargetTags())
                .add("targetFilterQueryId", getTargetFilterQueryId()).toString();
    }
}
