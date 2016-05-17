/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring data repositories for {@link TargetFilterQuery}s.
 *
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
public interface TargetFilterQueryRepository
        extends BaseEntityRepository<TargetFilterQuery, Long>, JpaSpecificationExecutor<TargetFilterQuery> {

    /**
     * Find customer target filter by name
     * 
     * @param name
     * @return custom target filter
     */
    TargetFilterQuery findByName(final String name);

    /**
     * Find list of all custom target filters.
     */
    @Override
    Page<TargetFilterQuery> findAll();

    @Override
    @Modifying
    @Transactional
    <S extends TargetFilterQuery> S save(S entity);

}
