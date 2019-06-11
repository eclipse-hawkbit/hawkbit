/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;

/**
 * Encapsulates a set of filters that may be specified (optionally). Properties
 * that are not specified (e.g. <code>null</code> for simple properties) When
 * applied, these filters are AND-gated.
 */
public class FilterParams implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Collection<TargetUpdateStatus> filterByStatus;
    private final boolean overdueState;
    private final String filterBySearchText;
    private final boolean selectTargetWithNoTag;
    private final String[] filterByTagNames;
    private final Long filterByDistributionId;

    private FilterParams(@NotNull Long filterByDistributionId) {
        this(Collections.emptyList(), null, null, filterByDistributionId, null);
    }

    /**
     * Constructor for creating filter-objects.
     *
     * @param filterByStatus
     *            if set, a filter is added for target states included by the
     *            collection
     * @param overdueState
     *            if set, a filter is added for overdued devices
     * @param filterBySearchText
     *            to find targets having the text anywhere in name or
     *            description.
     * @param filterByInstalledOrAssignedDistributionSetId
     *            filter by installed or assigned
     *            {@link DistributionSet#getId()}
     * @param selectTargetWithNoTag
     *            flag to select targets with no tag assigned, if set,
     *            tag-filtering is enabled
     * @param filterByTagNames
     *            if tag-filtering is enabled, a filter is added for the given
     *            tag-names
     */
    public FilterParams(final Collection<TargetUpdateStatus> filterByStatus, final Boolean overdueState,
            final String filterBySearchText, final Long filterByInstalledOrAssignedDistributionSetId,
            final Boolean selectTargetWithNoTag, final String... filterByTagNames) {
        this.filterByStatus = (filterByStatus == null) ? Collections.emptyList() : filterByStatus;
        this.overdueState = overdueState;
        this.filterBySearchText = filterBySearchText;
        this.filterByDistributionId = filterByInstalledOrAssignedDistributionSetId;
        this.selectTargetWithNoTag = (selectTargetWithNoTag == null) ? false : selectTargetWithNoTag;
        this.filterByTagNames = (filterByTagNames == null) ? new String[0] : filterByTagNames;
    }

    public static FilterParams forDistributionSet(Long distributionId) {
        return new FilterParams(distributionId);
    }

    public static FilterParams forTags(String... tagNames) {
        return new FilterParams(null, null, null, null, false, tagNames);
    }

    /**
     * Gets {@link DistributionSet#getId()} to filter the result. <br>
     * If set to <code>null</code> this filter is disabled.
     *
     * @return {@link DistributionSet#getId()} to filter the result
     */
    public Optional<Long> getFilterByDistributionId() {
        return Optional.ofNullable(filterByDistributionId);
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
    public Optional<Boolean> getOverdueState() {
        return Optional.ofNullable(overdueState);
    }

    /**
     * Gets the search text to filter for. This is used to find targets having
     * the text anywhere in name or description <br>
     * If set to <code>null</code> this filter is disabled.
     *
     * @return the search text to filter for
     */
    public Optional<String> getFilterBySearchText() {
        return Optional.ofNullable(filterBySearchText);
    }

    /**
     * Gets the flag indicating if tagging filter is used. <br>
     * If set to <code>null</code> this filter is disabled.
     *
     * @return the flag indicating if tagging filter is used
     */
    public boolean isSelectTargetWithNoTag() {
        return selectTargetWithNoTag;
    }

    /**
     * Gets the tags that are used to filter for. The activation of this filter
     * is done by {@link #isSelectTargetWithNoTag()}.
     *
     * @return the tags that are used to filter for or an empty array if non are
     *         present
     */
    public String[] getFilterByTagNames() {
        return filterByTagNames;
    }

    /**
     * checks if there is a valid filter specified
     * 
     * @return true if there is no valid filter set
     */
    public boolean isEmpty() {
        return isEmpty(filterByDistributionId);
    }

    /**
     * Checks if there is a valid filter set. The filter is empty if no filter
     * criteria is set.
     * 
     * @param filterByDistributionId
     *            {@link DistributionSet#getId()}
     * @return true if there is no valid filter set.
     */
    public boolean isEmpty(Long filterByDistributionId) {
        return filterByStatus.isEmpty() || getFilterBySearchText().isPresent() || filterByDistributionId != null
                || !hasTagsFilterActive();
    }

    /**
     * Check if a filter by tag should be applied. Returns also true when tags
     * should be explicitly ignored.
     * 
     * @return true when a tag-filter should be applied, false otherwise
     */
    public boolean hasTagsFilterActive() {
        return selectTargetWithNoTag || filterByTagNames.length > 0;
    }
}
