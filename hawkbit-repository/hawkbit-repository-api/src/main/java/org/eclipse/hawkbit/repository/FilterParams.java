/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import java.util.Collection;

import lombok.Data;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;

/**
 * Encapsulates a set of filters that may be specified (optionally). Properties
 * that are not specified (e.g. <code>null</code> for simple properties) When
 * applied, these filters are AND-gated.
 */
@Data
public class FilterParams {

    private final Collection<TargetUpdateStatus> filterByStatus;
    private final Boolean overdueState;
    private final String filterBySearchText;
    private final Boolean selectTargetWithNoTag;
    private final String[] filterByTagNames;
    private final Long filterByDistributionId;
    private final Boolean selectTargetWithNoTargetType;
    private final Long filterByTargetType;

    /**
     * Constructor for the filter parameters of a Simple Filter.
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
        this.selectTargetWithNoTargetType = false;
        this.filterByTargetType = null;
    }

    /**
     * Constructor for the filter parameters of a Type Filter.
     *
     * @param filterBySearchText
     *            if set, a filter is added for the given search text
     * @param filterByInstalledOrAssignedDistributionSetId
     *            if set, a filter is added for the given
     *            {@link DistributionSet#getId()}
     * @param selectTargetWithNoType
     *            if true, a filter is added with no type
     * @param filterByType
     *            if set, a filter is added for the given target type
     */
    public FilterParams(final String filterBySearchText, final Long filterByInstalledOrAssignedDistributionSetId,
                        final Boolean selectTargetWithNoType, final Long filterByType) {
        this.filterBySearchText = filterBySearchText;
        this.filterByDistributionId = filterByInstalledOrAssignedDistributionSetId;
        this.filterByStatus = null;
        this.overdueState = null;
        this.selectTargetWithNoTag = false;
        this.filterByTagNames = null;
        this.selectTargetWithNoTargetType = selectTargetWithNoType;
        this.filterByTargetType = filterByType;
    }
}
