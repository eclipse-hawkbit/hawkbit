/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

/**
 * Interface for the entity interceptor lifecycle.
 */
// Exception squid:EmptyStatementUsageCheck - don't want to force users to
// implement all methods
@SuppressWarnings("squid:EmptyStatementUsageCheck")
public interface EntityInterceptor {

    /**
     * Callback for the {@link PrePersist} lifecycle event.
     *
     * @param entity the model entity
     */
    default void prePersist(final Object entity) {
    }

    /**
     * Callback for the {@link PostPersist} lifecycle event.
     *
     * @param entity the model entity
     */
    default void postPersist(final Object entity) {
    }

    /**
     * Callback for the {@link PostRemove} lifecycle event.
     *
     * @param entity the model entity
     */
    default void postRemove(final Object entity) {
    }

    /**
     * Callback for the {@link PreRemove} lifecycle event.
     *
     * @param entity the model entity
     */
    default void preRemove(final Object entity) {
    }

    /**
     * Callback for the {@link PostLoad} lifecycle event.
     *
     * @param entity the model entity
     */
    default void postLoad(final Object entity) {
    }

    /**
     * Callback for the {@link PreUpdate} lifecycle event.
     *
     * @param entity the model entity
     */
    default void preUpdate(final Object entity) {
    }

    /**
     * Callback for the {@link PostUpdate} lifecycle event.
     *
     * @param entity the model entity
     */
    default void postUpdate(final Object entity) {
    }

}
