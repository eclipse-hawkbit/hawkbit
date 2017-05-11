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

import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
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
public interface TargetTagRepository
        extends BaseEntityRepository<JpaTargetTag, Long>, JpaSpecificationExecutor<JpaTargetTag> {

    /**
     * deletes the {@link TargetTag}s with the given tag names.
     *
     * @param tagName
     *            to be deleted
     * @return 1 if tag was deleted
     */
    @Modifying
    @Transactional
    Long deleteByName(String tagName);

    /**
     * find {@link TargetTag} by its name.
     *
     * @param tagName
     *            to filter on
     * @return the {@link TargetTag} if found, otherwise null
     */
    Optional<TargetTag> findByNameEquals(String tagName);

    /**
     * Checks if tag with given name exists.
     * 
     * @param tagName
     *            to check for
     * @return <code>true</code> is tag with given name exists
     */
    @Query("SELECT CASE WHEN COUNT(t)>0 THEN 'true' ELSE 'false' END FROM JpaTargetTag t WHERE t.name=:tagName")
    boolean existsByName(@Param("tagName") String tagName);

    /**
     * Returns all instances of the type.
     *
     * @return all entities
     */
    @Override
    List<JpaTargetTag> findAll();

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
    @Query("DELETE FROM JpaTargetTag t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);
}
