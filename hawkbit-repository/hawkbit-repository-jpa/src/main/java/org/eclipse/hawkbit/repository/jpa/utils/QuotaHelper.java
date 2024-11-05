/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.utils;

import java.util.function.ToLongFunction;

import jakarta.validation.constraints.NotNull;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;

/**
 * Helper class to check quotas.
 */
@Slf4j
public final class QuotaHelper {

    private static final String MAX_ASSIGNMENT_QUOTA_EXCEEDED = "Quota exceeded: Cannot assign %s entities at once. The maximum is %s.";

    private QuotaHelper() {
        // no need to instantiate this class
    }

    /**
     * Asserts the specified assignment quota.
     *
     * @param requested The number of entities that shall be assigned to the parent
     *         entity.
     * @param limit The maximum number of entities that may be assigned to the
     *         parent entity.
     * @param type The type of the entities that shall be assigned.
     * @param parentType The type of the parent entity.
     * @throws AssignmentQuotaExceededException if the assignment operation would cause the quota to be
     *         exceeded
     */
    public static void assertAssignmentQuota(final long requested, final long limit, @NotNull final Class<?> type,
            @NotNull final Class<?> parentType) {
        assertAssignmentQuota(null, requested, limit, type.getSimpleName(), parentType.getSimpleName(), null);
    }

    /**
     * Asserts the specified assignment quota.
     *
     * @param parentId The ID of the parent entity.
     * @param requested The number of entities that shall be assigned to the parent
     *         entity.
     * @param limit The maximum number of entities that may be assigned to the
     *         parent entity.
     * @param type The type of the entities that shall be assigned.
     * @param parentType The type of the parent entity.
     * @param countFct Function to count the entities that are currently assigned to
     *         the parent entity.
     * @throws AssignmentQuotaExceededException if the assignment operation would cause the quota to be
     *         exceeded
     */
    public static <T> void assertAssignmentQuota(final T parentId, final long requested, final long limit,
            @NotNull final Class<?> type, @NotNull final Class<?> parentType, final ToLongFunction<T> countFct) {
        assertAssignmentQuota(parentId, requested, limit, type.getSimpleName(), parentType.getSimpleName(), countFct);
    }

    /**
     * Asserts the specified assignment quota.
     *
     * @param parentId The ID of the parent entity.
     * @param requested The number of entities that shall be assigned to the parent
     *         entity.
     * @param limit The maximum number of entities that may be assigned to the
     *         parent entity.
     * @param type The type of the entities that shall be assigned.
     * @param parentType The type of the parent entity.
     * @param countFct Function to count the entities that are currently assigned to
     *         the parent entity.
     * @throws AssignmentQuotaExceededException if the assignment operation would cause the quota to be
     *         exceeded
     */
    public static <T> void assertAssignmentQuota(final T parentId, final long requested, final long limit,
            @NotNull final String type, @NotNull final String parentType, final ToLongFunction<T> countFct) {

        // check if the quota is unlimited
        if (limit <= 0) {
            log.debug("Quota 'Max {} entities per {}' is unlimited.", type, parentType);
            return;
        }

        if (requested > limit) {
            final String parentIdStr = parentId != null ? String.valueOf(parentId) : "<new>";
            log.warn("Cannot assign {} {} entities to {} '{}' because of the configured quota limit {}.", requested,
                    type, parentType, parentIdStr, limit);
            throw new AssignmentQuotaExceededException(type, parentType, parentId, requested, limit);
        }

        if (parentId != null && countFct != null) {
            final long currentCount = countFct.applyAsLong(parentId);
            if (currentCount + requested > limit) {
                log.warn(
                        "Cannot assign {} {} entities to {} '{}' because of the configured quota limit {}. Currently, there are {} {} entities assigned.",
                        requested, type, parentType, parentId, limit, currentCount, type);
                throw new AssignmentQuotaExceededException(type, parentType, parentId, requested, limit);
            }
        }
    }

    /**
     * Assert that the number of assignments in a request does not exceed the
     * limit.
     *
     * @param requested the number of assignments that are to be made
     * @param limit the maximum number of assignments per request
     */
    public static void assertAssignmentRequestSizeQuota(final long requested, final long limit) {
        if (requested > limit) {
            final String message = String.format(MAX_ASSIGNMENT_QUOTA_EXCEEDED, requested, limit);
            log.warn(message);
            throw new AssignmentQuotaExceededException(message);
        }
    }
}
