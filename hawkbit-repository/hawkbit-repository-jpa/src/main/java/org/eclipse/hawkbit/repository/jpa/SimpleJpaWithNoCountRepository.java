/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.io.Serializable;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.lang.Nullable;

/**
 * Repository implementation that allows findAll with disabled count query.
 * 
 * @param <T>
 *            entity type
 * @param <I>
 *            key or ID type
 */
public class SimpleJpaWithNoCountRepository<T, I extends Serializable> extends SimpleJpaRepository<T, I>
        implements NoCountSliceRepository<T> {

    public SimpleJpaWithNoCountRepository(final Class<T> domainClass, final EntityManager em) {
        super(domainClass, em);
    }

    public SimpleJpaWithNoCountRepository(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
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

    protected <S extends T> Page<S> readPageWithoutCount(final TypedQuery<S> query, final Pageable pageable) {
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        final List<S> content = query.getResultList();

        return new PageImpl<>(content, pageable, content.size());
    }
}