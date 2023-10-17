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

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;

import org.eclipse.hawkbit.repository.BaseRepositoryTypeProvider;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Repository implementation that allows findAll with disabled count query.
 * 
 * @param <T>
 *            entity type
 * @param <I>
 *            key or ID type
 */
public class HawkBitBaseRepository<T, I extends Serializable> extends SimpleJpaRepository<T, I>
        implements NoCountSliceRepository<T>, ACMRepository<T> {

    public HawkBitBaseRepository(final Class<T> domainClass, final EntityManager em) {
        super(domainClass, em);
    }

    public HawkBitBaseRepository(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
    }

    @Override
    public Slice<T> findAllWithoutCount(@Nullable Specification<T> spec, Pageable pageable) {
        TypedQuery<T> query = getQuery(spec, pageable);
        return pageable.isUnpaged() ? new PageImpl<>(query.getResultList()) : readPageWithoutCount(query, pageable);
    }

    @Override
    public Slice<T> findAllWithoutCount(final Pageable pageable) {
        return findAllWithoutCount(null, pageable);
    }


    @Override
    @Transactional
    @NonNull
    public <S  extends T> S save(@NonNull AccessController.Operation operation, @NonNull final S entity) {
        return save(entity);
    }

    @Override
    @Transactional
    public <S extends T> List<S> saveAll(@NonNull AccessController.Operation operation, final Iterable<S> entities) {
        return saveAll(entities);
    }


    @NonNull
    public Optional<T> findOne(@NonNull AccessController.Operation operation, @Nullable Specification<T> spec) {
        return findOne(spec);
    }

    @Override
    @NonNull
    public List<T> findAll(@NonNull final AccessController.Operation operation, @Nullable final Specification<T> spec) {
        return findAll(spec);
    }

    @Override
    @NonNull
    public boolean exists(@NonNull AccessController.Operation operation, Specification<T> spec) {
        return exists(spec);
    }

    @Override
    @NonNull
    public long count(@NonNull final AccessController.Operation operation, @Nullable final Specification<T> spec) {
        return count(spec);
    }

    @NonNull
    @Override
    public Slice<T> findAllWithoutCount(@NonNull final AccessController.Operation operation, @Nullable Specification<T> spec, Pageable pageable) {
        return findAllWithoutCount(spec, pageable);
    }

    private <S extends T> Page<S> readPageWithoutCount(final TypedQuery<S> query, final Pageable pageable) {
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        final List<S> content = query.getResultList();

        return new PageImpl<>(content, pageable, content.size());
    }

    /**
     * Simple implementation of {@link BaseRepositoryTypeProvider} leveraging our
     * {@link HawkBitBaseRepository} for all current use cases
     */
    public static class RepositoryTypeProvider implements BaseRepositoryTypeProvider {

        @Override
        public Class<?> getBaseRepositoryType(final Class<?> repositoryType) {
            return HawkBitBaseRepository.class;
        }

    }
}