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
     * @param tenant
     *            to delete data from
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM JpaDistributionSetType t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);

    /**
     * Retrieves the {@link DistributionSetType}s for the given IDs. Workaround
     * for https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477
     *
     * @param ids
     *            of the types to be located
     * 
     * @return a list of distribution set types
     */
    @Override
    @Query("SELECT d FROM JpaDistributionSetType d WHERE d.id IN ?1")
    List<JpaDistributionSetType> findAllById(Iterable<Long> ids);

    /**
     * Counts the {@link SoftwareModuleType}s which are associated with the
     * addressed {@link DistributionSetType}.
     * 
     * @param id
     *            of the distribution set type
     * 
     * @return the number of associated software module types
     */
    @Query("SELECT COUNT (e.smType) FROM DistributionSetTypeElement e WHERE e.dsType.id = :id")
    long countSmTypesById(@Param("id") Long id);

}
