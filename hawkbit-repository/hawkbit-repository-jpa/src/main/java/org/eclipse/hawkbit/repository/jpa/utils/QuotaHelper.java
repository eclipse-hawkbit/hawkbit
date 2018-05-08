/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.utils;

import java.util.function.Function;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.exception.QuotaExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;

/**
 * Helper class to check assignment quotas.
 */
@Validated
public final class QuotaHelper {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(QuotaHelper.class);

    private QuotaHelper() {
        // no need to instantiate this class
    }

    /**
     * Asserts the specified assignment quota.
     * 
     * @param requested
     *            The number of entities that shall be assigned to the parent
     *            entity.
     * @param limit
     *            The maximum number of entities that may be assigned to the
     *            parent entity.
     * @param type
     *            The type of the entities that shall be assigned.
     * @param parentType
     *            The type of the parent entity.
     * 
     * @throws QuotaExceededException
     *             if the assignment operation would cause the quota to be
     *             exceeded
     */
    public static void assertAssignmentQuota(final long requested, final long limit, @NotNull final Class<?> type,
            @NotNull final Class<?> parentType) {
        assertAssignmentQuota(null, requested, limit, type.getSimpleName(), parentType.getSimpleName(), null);
    }

    /**
     * Asserts the specified assignment quota.
     * 
     * @param parentId
     *            The ID of the parent entity.
     * @param requested
     *            The number of entities that shall be assigned to the parent
     *            entity.
     * @param limit
     *            The maximum number of entities that may be assigned to the
     *            parent entity.
     * @param type
     *            The type of the entities that shall be assigned.
     * @param parentType
     *            The type of the parent entity.
     * @param countFct
     *            Function to count the entities that are currently assigned to
     *            the parent entity.
     * 
     * @throws QuotaExceededException
     *             if the assignment operation would cause the quota to be
     *             exceeded
     */
    public static void assertAssignmentQuota(final Long parentId, final long requested, final long limit,
            @NotNull final Class<?> type, @NotNull final Class<?> parentType, final Function<Long, Long> countFct) {
        assertAssignmentQuota(parentId, requested, limit, type.getSimpleName(), parentType.getSimpleName(), countFct);
    }

    /**
     * Asserts the specified assignment quota.
     * 
     * @param parentId
     *            The ID of the parent entity.
     * @param requested
     *            The number of entities that shall be assigned to the parent
     *            entity.
     * @param limit
     *            The maximum number of entities that may be assigned to the
     *            parent entity.
     * @param type
     *            The type of the entities that shall be assigned.
     * @param parentType
     *            The type of the parent entity.
     * @param countFct
     *            Function to count the entities that are currently assigned to
     *            the parent entity.
     * 
     * @throws QuotaExceededException
     *             if the assignment operation would cause the quota to be
     *             exceeded
     */
    public static void assertAssignmentQuota(final Long parentId, final long requested, final long limit,
            @NotNull final String type, @NotNull final String parentType, final Function<Long, Long> countFct) {

        // check if the quota is unlimited
        if (limit <= 0) {
            LOG.debug("Quota 'Max {} entities per {}' is unlimited.", type, parentType);
            return;
        }

        if (requested > limit) {
            final String parentIdStr = parentId != null ? String.valueOf(parentId) : "<new>";
            LOG.warn("Cannot assign {} {} entities to {} '{}' because of the configured quota limit {}.", requested,
                    type, parentType, parentIdStr, limit);
            throw new QuotaExceededException(type, parentType, parentId, requested, limit);
        }

        if (parentId != null && countFct != null) {
            final long currentCount = countFct.apply(parentId);
            if (currentCount + requested > limit) {
                LOG.warn(
                        "Cannot assign {} {} entities to {} '{}' because of the configured quota limit {}. Currently, there are {} {} entities assigned.",
                        requested, type, parentType, parentId, limit, currentCount, type);
                throw new QuotaExceededException(type, parentType, parentId, requested, limit);
            }
        }
    }
}
