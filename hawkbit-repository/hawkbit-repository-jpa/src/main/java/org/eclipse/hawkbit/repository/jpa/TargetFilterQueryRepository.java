/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Optional;

import org.eclipse.hawkbit.repository.jpa.model.JpaTargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring data repositories for {@link TargetFilterQuery}s.
 *
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
public interface TargetFilterQueryRepository
        extends BaseEntityRepository<JpaTargetFilterQuery, Long>, JpaSpecificationExecutor<JpaTargetFilterQuery> {

    /**
     * Find customer target filter by name
     * 
     * @param name
     * @return custom target filter
     */
    Optional<TargetFilterQuery> findByName(final String name);

    /**
     * Find list of all custom target filters.
     */
    @Override
    Page<JpaTargetFilterQuery> findAll();

    /**
     * Sets the auto assign distribution sets to null which match the ds ids.
     *
     * @param dsIds
     *            distribution set ids to be set to null
     */
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Query("update JpaTargetFilterQuery d set d.autoAssignDistributionSet = NULL where d.autoAssignDistributionSet in :ids")
    void unsetAutoAssignDistributionSet(@Param("ids") Long... dsIds);

}
