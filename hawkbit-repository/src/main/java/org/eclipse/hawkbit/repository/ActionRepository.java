/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Collection;
import java.util.List;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link Action} repository.
 *
 *
 *
 *
 *
 */
@Transactional(readOnly = true)
public interface ActionRepository extends BaseEntityRepository<Action, Long>, JpaSpecificationExecutor<Action> {

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.repository.CrudRepository#findAll()
     */
    @Override
    @EntityGraph(value = "Action.all", type = EntityGraphType.LOAD)
    Iterable<Action> findAll();

    /**
     * Retrieves an Action with all lazy attributes.
     * 
     * @param actionId
     *            the ID of the action
     * @return the found {@link Action}
     */
    @EntityGraph(value = "Action.all", type = EntityGraphType.LOAD)
    Action findById(Long actionId);

    /**
     * Retrieves all {@link Action}s which are referring the given
     * {@link DistributionSet}.
     *
     * @param pageable
     *            page parameters
     * @param ds
     *            the {@link DistributionSet} on which will be filtered
     * @return the found {@link Action}s
     */
    @EntityGraph(value = "Action.all", type = EntityGraphType.LOAD)
    Page<Action> findByDistributionSet(final Pageable pageable, final DistributionSet ds);

    /**
     * Retrieves all {@link Action}s which are referring the given
     * {@link Target}.
     * 
     * @param pageable
     *            page parameters
     * @param target
     *            the target to find assigned actions
     * @return the found {@link Action}s
     */
    Slice<Action> findByTarget(Pageable pageable, Target target);

    /**
     * Retrieves all {@link Action}s which are active and referring the given
     * {@link Target} in a specified order.
     * 
     * @param pageable
     *            page parameters
     * @param target
     *            the target to find assigned actions
     * @param active
     *            the action active flag
     * @return the found {@link Action}s
     */
    @EntityGraph(value = "Action.ds", type = EntityGraphType.LOAD)
    List<Action> findByTargetAndActiveOrderByIdAsc(final Target target, boolean active);

    /**
     * Retrieves latest {@link UpdateAction} for given target and
     * {@link SoftwareModule}.
     *
     * @param targetId
     *            to search for
     * @param module
     *            to search for
     * @return action if there is one with assigned target and module is part of
     *         assigned {@link DistributionSet}.
     */
    @Query("Select a from Action a join a.distributionSet ds join ds.modules modul where a.target.controllerId = :target and modul = :module order by a.id desc")
    List<Action> findActionByTargetAndSoftwareModule(@Param("target") final String targetId,
            @Param("module") SoftwareModule module);

    /**
     * Retrieves all {@link UpdateAction}s which are referring the given
     * {@link DistributionSet} and {@link Target}.
     *
     * @param pageable
     *            page parameters
     * @param target
     *            is the assigned target
     * @param ds
     *            the {@link DistributionSet} on which will be filtered
     * @return the found {@link UpdateAction}s
     */
    @Query("Select a from Action a where a.target = :target and a.distributionSet = :ds order by a.id")
    @EntityGraph(value = "Action.all", type = EntityGraphType.LOAD)
    Page<Action> findByTargetAndDistributionSet(final Pageable pageable, @Param("target") final Target target,
            @Param("ds") DistributionSet ds);

    /**
     * Retrieves all {@link Action}s of a specific target, without pagination
     * ordered by action ID.
     * 
     * @param target
     *            to search for
     * @return a list of actions according to the searched target
     */
    @Query("Select a from Action a where a.target = :target order by a.id")
    List<Action> findByTarget(@Param("target") final Target target);

    /**
     * Retrieves all {@link Action}s of a specific target and given active flag
     * ordered by action ID.
     * 
     * @param pageable
     *            the pagination parameter
     * @param target
     *            to search for
     * @param active
     *            {@code true} for all actions which are currently active,
     *            {@code false} for inactive
     * @return a paged list of actions ordered by action ID
     */
    @Query("Select a from Action a where a.target = :target and a.active= :active order by a.id")
    Page<Action> findByActiveAndTarget(Pageable pageable, @Param("target") Target target,
            @Param("active") boolean active);

    /**
     * Retrieves all {@link Action}s of a specific target and given active flag
     * ordered by action ID.
     * 
     * @param target
     *            to search for
     * @param active
     *            {@code true} for all actions which are currently active,
     *            {@code false} for inactive
     * @return a list of actions ordered by action ID
     */
    @EntityGraph(value = "Action.ds", type = EntityGraphType.LOAD)
    @Query("Select a from Action a where a.target = :target and a.active= :active order by a.id")
    List<Action> findByActiveAndTarget(@Param("target") Target target, @Param("active") boolean active);

    /**
     * Updates all {@link Action} to inactive for all targets with given ID.
     * 
     * @param keySet
     *            the list of actions to set inactive
     * @param targetsIds
     *            the IDs of the targets according to the action to set in
     *            active
     */
    @Modifying
    @Transactional
    @Query("UPDATE Action a SET a.active = false WHERE a IN :keySet AND a.target IN :targetsIds")
    void setToInactive(@Param("keySet") List<Action> keySet, @Param("targetsIds") List<Long> targetsIds);

    /**
     * Retrieves all {@link Action}s which are active and referring to the given
     * target Ids and distribution set required migration step.
     *
     * @param targetIds
     *            the IDs of targets for the actions
     * @param notStatus
     *            the status which the actions should not have
     * @return the found list of {@link Action}s
     */
    @Query("SELECT a FROM Action a WHERE a.active = true AND a.distributionSet.requiredMigrationStep = false AND a.target IN ?1 AND a.status != ?2")
    List<Action> findByActiveAndTargetIdInAndActionStatusNotEqualToAndDistributionSetRequiredMigrationStep(
            Collection<Long> targetIds, Action.Status notStatus);

    /**
     * Counts all {@link Action}s referring to the given target.
     * 
     * @param target
     *            the target to count the {@link Action}s
     * @return the count of actions referring to the given target
     */
    Long countByTarget(Target target);

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.repository.CrudRepository#save(java.lang.
     * Iterable)
     */
    @Override
    @CacheEvict(value = "feedbackReceivedOverTime", allEntries = true)
    <S extends Action> List<S> save(Iterable<S> entities);

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.CrudRepository#save(java.lang.Object)
     */
    @Override
    @CacheEvict(value = "feedbackReceivedOverTime", allEntries = true)
    <S extends Action> S save(S entity);

    /**
     * Counts all {@link Action}s referring to the given DistributionSet.
     * 
     * @param distributionSet
     *            DistributionSet to count the {@link Action}s from
     * @return the count of actions referring to the given target
     */
    Long countByDistributionSet(DistributionSet distributionSet);
}
