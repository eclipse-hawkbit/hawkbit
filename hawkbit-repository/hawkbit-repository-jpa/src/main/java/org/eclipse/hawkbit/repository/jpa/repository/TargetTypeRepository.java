/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.repository;

import java.util.List;

import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetTypeSpecification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link PagingAndSortingRepository} and {@link org.springframework.data.repository.CrudRepository} for {@link JpaTargetType}.
 */
@Transactional(readOnly = true)
public interface TargetTypeRepository extends BaseEntityRepository<JpaTargetType> {

    default List<JpaTargetType> findByDsType(@Param("id") final Long dsTypeId) {
        return findAll(TargetTypeSpecification.hasDsSetType(dsTypeId));
    }

    @Modifying
    @Transactional
    @Query("DELETE FROM JpaTargetType t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);
}