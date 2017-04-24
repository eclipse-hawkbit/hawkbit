/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.io.Serializable;
import java.util.Optional;

import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaTenantAwareBaseEntity;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command repository operations for all {@link TenantAwareBaseEntity}s.
 *
 * @param <T>
 *            type if the entity type
 * @param <I>
 *            of the entity type
 */
@NoRepositoryBean
@Transactional(readOnly = true)
public interface BaseEntityRepository<T extends AbstractJpaTenantAwareBaseEntity, I extends Serializable>
        extends PagingAndSortingRepository<T, I> {

    /**
     * Retrieves an {@link BaseEntity} by its id.
     * 
     * @param id
     *            to search for
     * @return {@link BaseEntity}
     */
    Optional<T> findById(I id);

}
