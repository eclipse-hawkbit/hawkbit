/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.repository;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

import org.eclipse.hawkbit.repository.BaseRepositoryTypeProvider;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * Repository implementation that allows findAll with disabled count query.
 *
 * @param <T> the domain type the repository manages
 * @param <ID> the type of the id of the entity the repository manages
 */
public class HawkbitBaseRepository<T, ID extends Serializable> extends SimpleJpaRepository<T, ID>
        implements JpaSpecificationEntityGraphExecutor<T>, NoCountSliceRepository<T>, ACMRepository<T> {

    private final EntityManager entityManager;

    public HawkbitBaseRepository(final Class<T> domainClass, final EntityManager entityManager) {
        super(domainClass, entityManager);
        this.entityManager = entityManager;
    }

    public HawkbitBaseRepository(final JpaEntityInformation<T, ?> entityInformation, final EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
    }

    @Override
    public Slice<T> findAllWithoutCount(final Pageable pageable) {
        return findAllWithoutCount(null, pageable);
    }

    @Override
    public Slice<T> findAllWithoutCount(@Nullable final Specification<T> spec, final Pageable pageable) {
        final TypedQuery<T> query = getQuery(spec, pageable);
        return pageable.isUnpaged() ? new PageImpl<>(query.getResultList()) : readPageWithoutCount(query, pageable);
    }

    @Override
    @Transactional
    @NonNull
    public <S extends T> S save(@Nullable AccessController.Operation operation, @NonNull final S entity) {
        return save(entity);
    }

    @Override
    @Transactional
    public <S extends T> List<S> saveAll(@Nullable AccessController.Operation operation, final Iterable<S> entities) {
        return saveAll(entities);
    }

    @NonNull
    public Optional<T> findOne(@Nullable AccessController.Operation operation, @Nullable Specification<T> spec) {
        return findOne(spec);
    }

    @Override
    @NonNull
    public List<T> findAll(@Nullable final AccessController.Operation operation, @Nullable final Specification<T> spec) {
        return findAll(spec);
    }

    @Override
    @NonNull
    public boolean exists(@Nullable AccessController.Operation operation, Specification<T> spec) {
        return exists(spec);
    }

    @Override
    @NonNull
    public long count(@Nullable final AccessController.Operation operation, @Nullable final Specification<T> spec) {
        return count(spec);
    }

    @Override
    public Optional<T> findOne(final Specification<T> spec, final String entityGraph) {
        try {
            return Optional.of(withEntityGraph(getQuery(spec, Sort.unsorted()), entityGraph).setMaxResults(2).getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<T> findAll(final Specification<T> spec, final String entityGraph) {
        return withEntityGraph(getQuery(spec, Sort.unsorted()), entityGraph).getResultList();
    }

    @Override
    public Page<T> findAll(final Specification<T> spec, final String entityGraph, final Pageable pageable) {
        final TypedQuery<T> query = withEntityGraph(getQuery(spec, pageable), entityGraph);
        return pageable.isUnpaged() ? new PageImpl<>(query.getResultList())
                : readPage(query, getDomainClass(), pageable, spec);
    }

    @Override
    public List<T> findAll(final Specification<T> spec, final String entityGraph, final Sort sort) {
        return withEntityGraph(getQuery(spec, sort), entityGraph).getResultList();
    }

    @NonNull
    @Override
    public Slice<T> findAllWithoutCount(@Nullable final AccessController.Operation operation, @Nullable Specification<T> spec,
            Pageable pageable) {
        return findAllWithoutCount(spec, pageable);
    }

    @Override
    @NonNull
    public Class<T> getDomainClass() {
        return super.getDomainClass();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '<' + getDomainClass().getSimpleName() + '>';
    }

    private TypedQuery<T> withEntityGraph(final TypedQuery<T> query, final String entityGraph) {
        final EntityGraph<?> graph = ObjectUtils.isEmpty(entityGraph) ? null : entityManager.createEntityGraph(entityGraph);
        return graph == null ? query : query.setHint("javax.persistence.loadgraph", graph);
    }

    private <S extends T> Page<S> readPageWithoutCount(final TypedQuery<S> query, final Pageable pageable) {
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        final List<S> content = query.getResultList();
        return new PageImpl<>(content, pageable, content.size());
    }

    /**
     * Simple implementation of {@link BaseRepositoryTypeProvider} leveraging our
     * {@link HawkbitBaseRepository} for all current use cases
     */
    public static class RepositoryTypeProvider implements BaseRepositoryTypeProvider {

        @Override
        public Class<?> getBaseRepositoryType(final Class<?> repositoryType) {
            return HawkbitBaseRepository.class;
        }
    }
}