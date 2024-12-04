/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import org.eclipse.hawkbit.repository.Identifiable;

/**
 * Core information of all entities.
 */
public interface BaseEntity extends Serializable, Identifiable<Long> {

    static Long getIdOrNull(final BaseEntity entity) {
        return entity == null ? null : entity.getId();
    }

    /**
     * @return user that created the {@link BaseEntity}.
     */
    String getCreatedBy();

    /**
     * @return time in {@link TimeUnit#MILLISECONDS} when the {@link BaseEntity} was created.
     */
    long getCreatedAt();

    /**
     * @return user that updated the {@link BaseEntity} last.
     */
    String getLastModifiedBy();

    /**
     * @return time in {@link TimeUnit#MILLISECONDS} when the {@link BaseEntity}
     *         was last time changed.
     */
    long getLastModifiedAt();

    /**
     * @return version of the {@link BaseEntity}.
     */
    int getOptLockRevision();
}