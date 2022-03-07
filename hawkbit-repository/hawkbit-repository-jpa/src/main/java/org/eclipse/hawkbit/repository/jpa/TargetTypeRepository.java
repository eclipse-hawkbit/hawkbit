/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetTypeSpecification;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link PagingAndSortingRepository} for {@link JpaTargetType}.
 *
 */
@Transactional(readOnly = true)
public interface TargetTypeRepository
        extends BaseEntityRepository<JpaTargetType, Long>, JpaSpecificationExecutor<JpaTargetType> {

    /**
     * Finds {@link TargetType} in the repository matching an id.
     *
     * Calls version based on spec to allow injecting further specs
     *
     * @param id
     *            id to filter for
     * @return {@link Optional} of {@link TargetType}
     */
    @Override
    @NonNull
    default Optional<JpaTargetType> findById(@NonNull final Long id) {
        return this.findOne(Specification.where(TargetTypeSpecification.hasId(id)));
    }

    /**
     * @param ids
     *            List of ID
     * @return Target type list
     */
    @Override
    @NonNull
    default List<JpaTargetType> findAllById(final Iterable<Long> ids) {
        final List<Long> collectedIds = StreamSupport.stream(ids.spliterator(), true).collect(Collectors.toList());
        return this.findAll(Specification.where(TargetTypeSpecification.hasIdIn(collectedIds)));
    }

    /**
     * Deletes the {@link TargetType}s with the given target IDs.
     *
     * @param targetTypeIDs
     *            to be deleted
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM JpaTargetType t WHERE t.id IN ?1")
    void deleteByIdIn(Collection<Long> targetTypeIDs);

    /**
     * Finds all {@link TargetType}s in the repository.
     *
     * Calls version with (empty) spec to allow injecting further specs
     *
     * @return {@link List} of {@link TargetType}s
     */
    @Override
    @NonNull
    default List<JpaTargetType> findAll() {
        return this.findAll(Specification.where(null));
    }

    /**
     * Finds all {@link TargetType}s in the repository sorted.
     *
     * Calls version with (empty) spec to allow injecting further specs
     *
     * @param sort
     *            instructions to sort result by
     * @return {@link List} of {@link TargetType}s
     */
    @Override
    @NonNull
    default Iterable<JpaTargetType> findAll(@NonNull final Sort sort) {
        return this.findAll(Specification.where(null), sort);
    }

    /**
     * Finds a page of {@link TargetType}s in the repository.
     *
     * Calls version with (empty) spec to allow injecting further specs
     *
     * @param pageable
     *            paging context
     * @return {@link List} of {@link TargetType}s
     */
    @Override
    @NonNull
    default Page<JpaTargetType> findAll(@NonNull final Pageable pageable) {
        return this.findAll(Specification.where(null), pageable);
    }

    /**
     * Checks whether {@link TargetType} in the repository matching an id exists
     * or not.
     *
     * Calls version based on spec to allow injecting further specs
     *
     * @param id
     *            id to check for
     * @return true if TargetType with id exists
     */
    @Override
    default boolean existsById(@NonNull final Long id) {
        return this.exists(TargetTypeSpecification.hasId(id));
    }

    /**
     * Checks whether {@link TargetType} in the repository matching a spec
     * exists or not.
     *
     * @param spec
     *            to check for existence
     * @return true if target with id exists
     */
    default boolean exists(@NonNull final Specification<JpaTargetType> spec) {
        return this.count(spec) > 0;
    }

    /**
     * Count number of {@link TargetType}s in the repository.
     *
     * Calls version with an empty spec to allow injecting further specs
     *
     * @return number of targetTypes in the repository
     */
    @Override
    default long count() {
        return this.count(Specification.where(null));
    }

    /**
     * @param tenant
     *            Tenant
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM JpaTargetType t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);

    @Query(value = "SELECT COUNT (t.id) FROM JpaDistributionSetType t JOIN t.compatibleToTargetTypes tt WHERE tt.id = :id")
    long countDsSetTypesById(@Param("id") Long id);

    /**
     *
     * @param dsTypeId
     *            to search for
     * @return all {@link TargetType}s in the repository with given
     *         {@link TargetType#getName()}
     */
    default List<JpaTargetType> findByDsType(@Param("id") final Long dsTypeId) {
        return this.findAll(Specification.where(TargetTypeSpecification.hasDsSetType(dsTypeId)));
    }

    /**
     *
     * @param name
     *            to search for
     * @return all {@link TargetType}s in the repository with given
     *         {@link TargetType#getName()}
     */
    default Optional<JpaTargetType> findByName(final String name) {
        return this.findOne(Specification.where(TargetTypeSpecification.hasName(name)));
    }

    /**
     * Count number of {@link TargetType}s in the repository by name.
     *
     * @param name
     *            target type name
     * @return number of targetTypes in the repository by name
     */
    default long countByName(final String name) {
        return this.count(TargetTypeSpecification.hasName(name));
    }
}
