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
 * Default implementation of the {@link AccessControlManager} permitting every
 * kind of request.
 * 
 * @param <T>
 *            the entity type to manage
 */
public class DefaultControlManager<T> implements AccessControlManager<T> {

    @Override
    public String serializeContext() {
        // nothing to serialize
        return null;
    }

    @Override
    public void runAsContext(String serializedContext, Runnable runnable) {
        // serializedContext will not set
        runnable.run();
    }

    /**
     * The default specification without limitation.
     * 
     * @return a new instance of {@link Specification} without limitations.
     */
    @Override
    public Specification<T> getAccessRules() {
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
    public Specification<T> appendAccessRules(final Specification<T> specification) {
        return specification;
    }

    @Override
    public void assertModificationAllowed(final List<T> entities, final Operation operation)
            throws InsufficientPermissionException {
        // Every request is allowed
    }

}
