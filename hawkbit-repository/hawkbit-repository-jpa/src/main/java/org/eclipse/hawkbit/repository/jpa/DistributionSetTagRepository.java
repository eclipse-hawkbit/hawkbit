/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link TargetTag} repository.
 *
 */
@Transactional(readOnly = true)
public interface DistributionSetTagRepository
        extends BaseEntityRepository<JpaDistributionSetTag, Long>, JpaSpecificationExecutor<JpaDistributionSetTag> {
    /**
     * deletes the {@link DistributionSet} with the given name.
     * 
     * @param tagName
     *            to be deleted
     * @return 1 if tag was deleted
     */
    @Modifying
    @Transactional
    Long deleteByName(final String tagName);

    /**
     * find {@link DistributionSetTag} by its name.
     * 
     * @param tagName
     *            to filter on
     * @return the {@link DistributionSetTag} if found, otherwise null
     */
    Optional<DistributionSetTag> findByNameEquals(final String tagName);

    /**
     * Checks if tag with given name exists.
     * 
     * @param tagName
     *            to check for
     * @return <code>true</code> is tag with given name exists
     */
    @Query("SELECT CASE WHEN COUNT(t)>0 THEN 'true' ELSE 'false' END FROM JpaDistributionSetTag t WHERE t.name=:tagName")
    boolean existsByName(@Param("tagName") String tagName);

    /**
     * Returns all instances of the type.
     *
     * @return all entities
     */
    @Override
    List<JpaDistributionSetTag> findAll();

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
    @Query("DELETE FROM JpaDistributionSetTag t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);

    @Override
    // Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477
    @Query("SELECT d FROM JpaDistributionSetTag d WHERE d.id IN ?1")
    List<JpaDistributionSetTag> findAll(Iterable<Long> ids);
}
