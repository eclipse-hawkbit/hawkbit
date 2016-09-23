/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Collection;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;

/**
 * Encapsulates a set of filters that may be specified (optionally). Properties
 * that are not specified (e.g. <code>null</code> for simple properties) When
 * applied, these filters are AND-gated.
 *
 */
public class FilterParams {

    Collection<TargetUpdateStatus> filterByStatus;
    Boolean overdueState;
    String filterBySearchText;
    Boolean selectTargetWithNoTag;
    String[] filterByTagNames;
    Long filterByDistributionId;

    /**
     * Constructor.
     *
     * @param filterByDistributionId
     *            if set, a filter is added for the given
     *            {@link DistributionSet#getId()}
     * @param filterByStatus
     *            if set, a filter is added for target states included by the
     *            collection
     * @param overdueState
     *            if set, a filter is added for overdued devices
     * @param filterBySearchText
     *            if set, a filter is added for the given search text
     * @param selectTargetWithNoTag
     *            if set, tag-filtering is enabled
     * @param filterByTagNames
     *            if tag-filtering is enabled, a filter is added for the given
     *            tag-names
     */
    public FilterParams(Long filterByDistributionId, Collection<TargetUpdateStatus> filterByStatus,
            Boolean overdueState, String filterBySearchText, Boolean selectTargetWithNoTag,
            String... filterByTagNames) {
        this.filterByStatus = filterByStatus;
        this.overdueState = overdueState;
        this.filterBySearchText = filterBySearchText;
        this.filterByDistributionId = filterByDistributionId;
        this.selectTargetWithNoTag = selectTargetWithNoTag;
        this.filterByTagNames = filterByTagNames;
    }

    /**
     * Gets {@link DistributionSet#getId()} to filter the result. <br>
     * If set to <code>null</code> this filter is disabled.
     *
     * @return {@link DistributionSet#getId()} to filter the result
     */
    public Long getFilterByDistributionId() {
        return filterByDistributionId;
    }

    /**
     * Sets {@link DistributionSet#getId()} to filter the result.
     *
     * @param filterByDistributionId
     */
    public void setFilterByDistributionId(Long filterByDistributionId) {
        this.filterByDistributionId = filterByDistributionId;
    }

    /**
     * Gets a collection of target states to filter for. <br>
     * If set to <code>null</code> this filter is disabled.
     *
     * @return collection of target states to filter for
     */
    public Collection<TargetUpdateStatus> getFilterByStatus() {
        return filterByStatus;
    }

    /**
     * Sets the collection of target states to filter for.
     *
     * @param filterByStatus
     */
    public void setFilterByStatus(Collection<TargetUpdateStatus> filterByStatus) {
        this.filterByStatus = filterByStatus;
    }

    /**
     * Gets the flag for overdue filter; if set to <code>true</code>, the
     * overdue filter is activated. Overdued targets a targets that did not
     * respond during the configured intervals: poll_itvl + overdue_itvl. <br>
     * If set to <code>null</code> this filter is disabled.
     *
     * @return flag for overdue filter activation
     */
    public Boolean getOverdueState() {
        return overdueState;
    }

    /**
     * Sets the flag for overdue filter; if set to <code>true</code>, the
     * overdue filter is activated.
     *
     * @param overdueState
     */
    public void setOverdueState(Boolean overdueState) {
        this.overdueState = overdueState;
    }

    /**
     * Gets the search text to filter for. This is used to find targets having
     * the text anywhere in name or description <br>
     * If set to <code>null</code> this filter is disabled.
     *
     * @return the search text to filter for
     */
    public String getFilterBySearchText() {
        return filterBySearchText;
    }

    /**
     * Sets the search text to filter for.
     *
     * @param filterBySearchText
     */
    public void setFilterBySearchText(String filterBySearchText) {
        this.filterBySearchText = filterBySearchText;
    }

    /**
     * Gets the flag indicating if tagging filter is used. <br>
     * If set to <code>null</code> this filter is disabled.
     *
     * @return the flag indicating if tagging filter is used
     */
    public Boolean getSelectTargetWithNoTag() {
        return selectTargetWithNoTag;
    }

    /**
     * Sets the flag indicating if tagging filter is used.
     *
     * @param selectTargetWithNoTag
     */
    public void setSelectTargetWithNoTag(Boolean selectTargetWithNoTag) {
        this.selectTargetWithNoTag = selectTargetWithNoTag;
    }

    /**
     * Gets the tags that are used to filter for. The activation of this filter
     * is done by {@link #setSelectTargetWithNoTag(Boolean)}.
     *
     * @return the tags that are used to filter for
     */
    public String[] getFilterByTagNames() {
        return filterByTagNames;
    }

    /**
     * Sets the tags that are used to filter for.
     *
     * @param filterByTagNames
     */
    public void setFilterByTagNames(String[] filterByTagNames) {
        this.filterByTagNames = filterByTagNames;
    }
}
