/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidationCount;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * A DistributionSetInvalidationManagement service provides operations to
 * invalidate {@link DistributionSet}s.
 *
 */
public interface DistributionSetInvalidationManagement {

    /**
     * Invalidates a given {@link DistributionSet}. The invalidation always
     * cancels all auto assignments referring this {@link DistributionSet} and
     * can not be undone. Optionally, all rollouts and actions referring this
     * {@link DistributionSet} can be canceled.
     *
     * @param distributionSetInvalidation
     *            defines the {@link DistributionSet} and options what should be
     *            canceled
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_UPDATE_REPOSITORY)
    public void invalidateDistributionSet(final DistributionSetInvalidation distributionSetInvalidation);

    /**
     * Counts all entities for a list of {@link DistributionSet}s that will be
     * canceled when invalidation is called for those {@link DistributionSet}s.
     *
     *
     * @param distributionSetInvalidation
     *            defines the {@link DistributionSet} and options what should be
     *            canceled
     * @return The {@link DistributionSetInvalidationCount} object that holds
     *         information about the count of affected rollouts,
     *         auto-assignments and actions
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    DistributionSetInvalidationCount countEntitiesForInvalidation(
            final DistributionSetInvalidation distributionSetInvalidation);

}
