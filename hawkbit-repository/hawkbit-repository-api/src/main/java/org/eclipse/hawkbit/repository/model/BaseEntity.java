/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.hateoas.Identifiable;

/**
 * Core information of all entities.
 *
 */
public interface BaseEntity extends Serializable, Identifiable<Long> {

    static Long getIdOrNull(final BaseEntity entity) {
        return Optional.ofNullable(entity).map(BaseEntity::getId).orElse(null);
    }

    /**
     * @return time in {@link TimeUnit#MILLISECONDS} when the {@link BaseEntity}
     *         was created.
     */
    Long getCreatedAt();

    /**
     * @return user that created the {@link BaseEntity}.
     */
    String getCreatedBy();

    /**
     * @return time in {@link TimeUnit#MILLISECONDS} when the {@link BaseEntity}
     *         was last time changed.
     */
    Long getLastModifiedAt();

    /**
     * @return user that updated the {@link BaseEntity} last.
     */
    String getLastModifiedBy();

    /**
     * @return version of the {@link BaseEntity}.
     */
    int getOptLockRevision();

}
