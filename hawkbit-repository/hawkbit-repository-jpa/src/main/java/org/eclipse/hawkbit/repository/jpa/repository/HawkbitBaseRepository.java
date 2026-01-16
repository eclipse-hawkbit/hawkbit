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
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

import org.eclipse.hawkbit.repository.jpa.acm.AccessController.Operation;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.util.ObjectUtils;

/**
 * Repository implementation that allows findAll with disabled count query.
 *
 * @param <T> the domain type the repository manages
 * @param <ID> the type of the id of the entity the repository manages
 */
@SuppressWarnings("java:S119") // inherited from SimpleJpaRepository
public class HawkbitBaseRepository<T, ID extends Serializable> extends SimpleJpaRepository<T, ID>
        implements JpaSpecificationEntityGraphExecutor<T>, NoCountSliceRepository<T>, ACMRepository<T> {

    private final EntityManager entityManager;
    private final Logger log;

    public HawkbitBaseRepository(final Class<T> domainClass, final EntityManager entityManager) {
        super(domainClass, entityManager);
        this.entityManager = entityManager;
        log = LoggerFactory.getLogger(getDomainClass());
    }

    public HawkbitBaseRepository(final JpaEntityInformation<T, ?> entityInformation, final EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
        log = LoggerFactory.getLogger(getDomainClass());
    }

    @Override
    public Slice<T> findAllWithoutCount(final Pageable pageable) {
        return findAllWithoutCount(Specification.unrestricted(), pageable);
    }

    @Override
    public Slice<T> findAllWithoutCount(@Nullable final Specification<T> spec, final Pageable pageable) {
        final TypedQuery<T> query = getQuery(spec, pageable);
        return pageable.isUnpaged() ? new PageImpl<>(query.getResultList()) : readPageWithoutCount(query, pageable);
    }

    @Override
    @Transactional
    @NonNull
    @SuppressWarnings("java:S6809") // this method already has a transactional annotation witch shall be applied
    public <S extends T> S save(final Operation operation, @NonNull final S entity) {
        return save(entity);
    }

    @Override
    @Transactional
    @SuppressWarnings("java:S6809") // this method already has a transactional annotation witch shall be applied
    public <S extends T> List<S> saveAll(final Operation operation, final Iterable<S> entities) {
        return saveAll(entities);
    }

    @NonNull
    public Optional<T> findOne(final Operation operation, @NonNull Specification<T> spec) {
        return findOne(spec);
    }

    @Override
    @NonNull
    @SuppressWarnings("java:S4449") // find all accepts null
    public List<T> findAll(@Nullable final Operation operation, @Nullable final Specification<T> spec) {
        return findAll(spec);
    }

    @Override
    public boolean exists(@Nullable Operation operation, Specification<T> spec) {
        return exists(spec);
    }

    @Override
    public long count(@Nullable final Operation operation, @Nullable final Specification<T> spec) {
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
    public Slice<T> findAllWithoutCount(@Nullable final Operation operation, @Nullable Specification<T> spec,
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
        if (ObjectUtils.isEmpty(entityGraph)) {
            return query;
        } else {
            final EntityGraph<?> graph = entityManager.createEntityGraph(entityGraph);
            if (graph == null) {
                log.warn("Entity graph {} not found", entityGraph);
                return query;
            } else {
                return query.setHint("jakarta.persistence.loadgraph", graph);
            }
        }
    }

    private <S extends T> Page<S> readPageWithoutCount(final TypedQuery<S> query, final Pageable pageable) {
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        final List<S> content = query.getResultList();
        return new PageImpl<>(content, pageable, content.size());
    }
}