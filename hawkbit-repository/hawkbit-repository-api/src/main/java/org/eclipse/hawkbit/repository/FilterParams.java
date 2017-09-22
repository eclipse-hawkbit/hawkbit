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

    private final Collection<TargetUpdateStatus> filterByStatus;
    private final Boolean overdueState;
    private final String filterBySearchText;
    private final Boolean selectTargetWithNoTag;
    private final String[] filterByTagNames;
    private final Long filterByDistributionId;

    /**
     * Constructor.
     *
     * @param filterByInstalledOrAssignedDistributionSetId
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
    public FilterParams(final Collection<TargetUpdateStatus> filterByStatus, final Boolean overdueState,
            final String filterBySearchText, final Long filterByInstalledOrAssignedDistributionSetId,
            final Boolean selectTargetWithNoTag, final String... filterByTagNames) {
        this.filterByStatus = filterByStatus;
        this.overdueState = overdueState;
        this.filterBySearchText = filterBySearchText;
        this.filterByDistributionId = filterByInstalledOrAssignedDistributionSetId;
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
     * Gets a collection of target states to filter for. <br>
     * If set to <code>null</code> this filter is disabled.
     *
     * @return collection of target states to filter for
     */
    public Collection<TargetUpdateStatus> getFilterByStatus() {
        return filterByStatus;
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
     * Gets the flag indicating if tagging filter is used. <br>
     * If set to <code>null</code> this filter is disabled.
     *
     * @return the flag indicating if tagging filter is used
     */
    public Boolean getSelectTargetWithNoTag() {
        return selectTargetWithNoTag;
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
}
