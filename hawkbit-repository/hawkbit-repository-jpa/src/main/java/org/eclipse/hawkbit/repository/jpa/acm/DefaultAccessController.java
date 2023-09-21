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

import java.util.List;

/**
 * Default implementation of the {@link AccessController} permitting every kind
 * of request.
 * 
 * @param <T>
 *            the entity type to manage
 */
public class DefaultAccessController<T> implements AccessController<T> {

    @Override
    public String getContext() {
        // nothing to serialize
        return null;
    }

    @Override
    public void runInContext(String serializedContext, Runnable runnable) {
        // serializedContext will not set
        runnable.run();
    }

    /**
     * The default specification without limitation.
     * 
     * @return a new instance of {@link Specification} without limitations.
     */
    @Override
    public Specification<T> getAccessRules(final Operation operation) {
        return Specification.where(null);
    }

    /**
     * Nothing to append.
     * 
     * @param specification
     *            is the root specification which needs to be appended by the
     *            resource limitation
     * @return the unmodified specification
     */
    @Override
    public Specification<T> appendAccessRules(final Operation operation, final Specification<T> specification) {
        return specification;
    }

    @Override
    public void assertOperationAllowed(final Operation operation, final List<T> entities)
            throws InsufficientPermissionException {
        // Every request is allowed
    }

}
