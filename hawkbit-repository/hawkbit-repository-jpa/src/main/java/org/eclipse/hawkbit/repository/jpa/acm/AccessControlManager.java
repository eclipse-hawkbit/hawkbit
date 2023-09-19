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

import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;

/**
 * Interface of an access control manager. Used by management layer to verify
 * the permission for CRUD operations based on some access criteria.
 * 
 * @param <T>
 */
public interface AccessControlManager<T> {

    /**
     * Introduce a new specification to limit the access to a specific entity.
     * 
     * @return a new specification limiting the access
     */
    Specification<T> getAccessRules();

    /**
     * Append the resource limitation on an already existing specification.
     * 
     * @param specification
     *            is the root specification which needs to be appended by the
     *            resource limitation
     * @return a new appended specification
     */
    Specification<T> appendAccessRules(Specification<T> specification);

    /**
     * Default implementation pointing to
     * {@link AccessControlManager#assertModificationAllowed(T, Operation)}
     *
     * @throws InsufficientPermissionException
     */
    default void assertModificationAllowed(T entity, Operation operation) throws InsufficientPermissionException {
        assertModificationAllowed(Collections.singletonList(entity), operation);
    }

    /**
     * Verify if given {@link Operation} is permitted for provided entities.
     * 
     * @param entities
     * @param operation
     * @throws InsufficientPermissionException
     */
    void assertModificationAllowed(List<T> entities, Operation operation) throws InsufficientPermissionException;

    /**
     * Enum to define the perform operation to verify
     */
    enum Operation {

        /**
         * Entity creation
         */
        CREATE,

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
