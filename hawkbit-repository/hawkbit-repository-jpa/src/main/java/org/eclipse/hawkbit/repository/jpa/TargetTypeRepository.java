/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * {@link PagingAndSortingRepository} for {@link JpaTargetType}.
 *
 */
@Transactional(readOnly = true)
public interface TargetTypeRepository
        extends BaseEntityRepository<JpaTargetType, Long>, JpaSpecificationExecutor<JpaTargetType> {

    Page<JpaTargetType> findByDeleted(Pageable pageable, boolean isDeleted);

    long countByDeleted(boolean isDeleted);

    long countByElementsDsSetType(JpaDistributionSetType distributionSetType);

    @Modifying
    @Transactional
    @Query("DELETE FROM JpaDistributionSetType t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);

    @Override
    @Query("SELECT d FROM JpaTargetType d WHERE d.id IN ?1")
    List<JpaTargetType> findAllById(Iterable<Long> ids);

    @Query("SELECT COUNT (e.dsType) FROM TargetTypeElement e WHERE e.targetType.id = :id")
    long countDsSetTypesById(@Param("id") Long id);

}
