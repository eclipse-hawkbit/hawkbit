/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.acm.controller;

import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.springframework.data.jpa.domain.Specification;

/**
 * Interface of an access control manager. Used by management layer to verify
 * the permission for CRUD operations based on some access criteria.
 * 
 * @param <T>
 */
public interface AccessController<T> {

    /**
     * Introduce a new specification to limit the access to a specific entity.
     *
     * @return a new specification limiting the access
     */
    Specification<T> getAccessRules(Operation operation);

    /**
     * Append the resource limitation on an already existing specification.
     * 
     * @param specification
     *            is the root specification which needs to be appended by the
     *            resource limitation
     * @return a new appended specification
     */
    default Specification<T> appendAccessRules(final Operation operation, final Specification<T> specification) {
        return specification.and(getAccessRules(operation));
    }

    /**
     * Default implementation pointing to
     * {@link AccessController#assertOperationAllowed(Operation, Object)}
     *
     * @throws InsufficientPermissionException
     *             if operation is not permitted for given entities
     */
    default void assertOperationAllowed(final Operation operation, final T entity) throws InsufficientPermissionException {
        assertOperationAllowed(operation, Collections.singletonList(entity));
    }

    /**
     * Verify if given {@link Operation} is permitted for provided entities.
     *
     * @throws InsufficientPermissionException
     *             if operation is not permitted for given entities
     */
    void assertOperationAllowed(Operation operation, List<T> entities) throws InsufficientPermissionException;

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
