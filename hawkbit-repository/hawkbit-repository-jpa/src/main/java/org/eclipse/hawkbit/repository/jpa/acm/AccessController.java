/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.acm;

import java.util.Optional;

import jakarta.persistence.criteria.Root;

import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.springframework.data.jpa.domain.DeleteSpecification;
import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.UpdateSpecification;
import org.jspecify.annotations.Nullable;

/**
 * Interface of an extended access control by providing means or fine-grained access control.
 * Used by repository (and on few places by management) layer to verify the permission for CRUD operations
 * based on some access criteria.
 * <p/>
 * First the basic service based access control (hawkBit permissions) on the management layer is applied then
 * additional restrictions (e.g. entity based) could be applied.
 * <p/>
 * <b>Note</b>: Experimental, only
 *
 * @param <T> the domain type the repository manages
 */
public interface AccessController<T> {

    /**
     * Introduce a new specification to limit the access to a specific entity.
     *
     * @return a new specification limiting the access, if empty no access restrictions are to be applied
     */
    Optional<Specification<T>> getAccessRules(Operation operation);

    /**
     * Append the resource limitation on an already existing specification.
     *
     * @param specification is the root specification which needs to be appended by the resource limitation
     * @return a new appended specification
     */
    @Nullable
    default Specification<T> appendAccessRules(final Operation operation, @Nullable final Specification<T> specification) {
        return getAccessRules(operation)
                .map(accessRules -> specification == null ? accessRules : specification.and(accessRules))
                .orElse(specification);
    }

    default UpdateSpecification<T> appendAccessRules(final Operation operation, @Nullable final UpdateSpecification<T> specification) {
        return getAccessRules(operation)
                .map(this::predicateSpec)
                .map(accessRules -> specification == null ? UpdateSpecification.where(accessRules) : specification.and(accessRules))
                .orElse(specification);
    }

    default DeleteSpecification<T> appendAccessRules(final Operation operation, @Nullable final DeleteSpecification<T> specification) {
        return getAccessRules(operation)
                .map(this::predicateSpec)
                .map(accessRules -> specification == null ? DeleteSpecification.where(accessRules) : specification.and(accessRules))
                .orElse(specification);
    }

    /**
     * Verify if the given {@link Operation} is permitted for the provided entity.
     *
     * @throws InsufficientPermissionException if operation is not permitted for given entities
     */
    void assertOperationAllowed(final Operation operation, final T entity) throws InsufficientPermissionException;

    /**
     * Verify if the given {@link Operation} is permitted for the provided entities.
     *
     * @throws InsufficientPermissionException if operation is not permitted for given entities
     */
    default void assertOperationAllowed(final Operation operation, final Iterable<? extends T> entities)
            throws InsufficientPermissionException {
        for (final T entity : entities) {
            assertOperationAllowed(operation, entity);
        }
    }

    @Deprecated
    default PredicateSpecification<T> predicateSpec(final Specification<T> spec) {
        return (from, cb) -> spec.toPredicate((Root<T>) from, cb.createQuery(), cb);
    }

    /**
     * Enum to define the perform operation to verify
     */
    enum Operation {

        /**
         * Entity creation
         */
        CREATE,
        /**
         * Read entities
         */
        READ,
        /**
         * Entity modification (e.g. name/description change, tag/type assignment, etc.)
         */
        UPDATE,
        /**
         * Entity deletion
         */
        DELETE
    }
}