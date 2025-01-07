/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidationCount;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * A DistributionSetInvalidationManagement service provides operations to
 * invalidate {@link DistributionSet}s.
 */
public interface DistributionSetInvalidationManagement {

    /**
     * Invalidates a given {@link DistributionSet}. The invalidation always
     * cancels all auto assignments referring this {@link DistributionSet} and
     * can not be undone. Optionally, all rollouts and actions referring this
     * {@link DistributionSet} can be canceled.
     * <p>
     * {@link PreAuthorize} missing intentionally as it relies on the permission set defined in the management api methods that it calls internally.
     *
     * @param distributionSetInvalidation defines the {@link DistributionSet} and options what should be
     *         canceled
     */
    void invalidateDistributionSet(final DistributionSetInvalidation distributionSetInvalidation);

    /**
     * Counts all entities for a list of {@link DistributionSet}s that will be
     * canceled when invalidation is called for those {@link DistributionSet}s.
     * <p>
     * {@link PreAuthorize} missing intentionally as it relies on the permission set defined in the management api methods that it calls internally.
     *
     * @param distributionSetInvalidation defines the {@link DistributionSet} and options what should be
     *         canceled
     * @return The {@link DistributionSetInvalidationCount} object that holds
     *         information about the count of affected rollouts,
     *         auto-assignments and actions
     */
    DistributionSetInvalidationCount countEntitiesForInvalidation(
            final DistributionSetInvalidation distributionSetInvalidation);

}
