/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.repository;

import java.util.List;

import javax.persistence.EntityManager;

import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link PagingAndSortingRepository} and {@link  org.springframework.data.repository.CrudRepository} for
 * {@link DistributionSetType}.
 *
 */
@Transactional(readOnly = true)
public interface DistributionSetTypeRepository
        extends BaseEntityRepository<JpaDistributionSetType> {

    /**
     * Counts all distribution set type where a specific software module type is
     * assigned to.
     * <p/>
     * No access control applied
     * 
     * @param softwareModuleType
     *            the software module type to count the distribution set type
     *            which has this software module type assigned
     * 
     * @return the number of {@link DistributionSetType}s in the repository
     *         assigned to the given software module type
     */
    long countByElementsSmType(JpaSoftwareModuleType softwareModuleType);

    /**
     * Deletes all {@link TenantAwareBaseEntity} of a given tenant. For safety
     * reasons (this is a "delete everything" query after all) we add the tenant
     * manually to query even if this will by done by {@link EntityManager}
     * anyhow. The DB should take care of optimizing this away.
     *
     * @param tenant to delete data from
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM JpaDistributionSetType t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);

    /**
     * Counts the {@link SoftwareModuleType}s which are associated with the
     * addressed {@link DistributionSetType}.
     * <p/>
     * No access control applied
     * 
     * @param id
     *            of the distribution set type
     * 
     * @return the number of associated software module types
     */
    @Query("SELECT COUNT (e.smType) FROM DistributionSetTypeElement e WHERE e.dsType.id = :id")
    long countSmTypesById(@Param("id") Long id);
}
