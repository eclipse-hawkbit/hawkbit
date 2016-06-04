/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Workaround as spring data does not provide a {@link Slice} based
 * {@link JpaRepository#findAll()}.
 *
 */
@Repository
public class NoCountPagingRepository {

    @Autowired
    protected EntityManager em;

    /**
     * Searches without the need for an extra count query.
     *
     * @param spec
     *            to search for
     * @param pageable
     *            information
     * @param domainClass
     *            of the {@link Entity}
     *
     * @return {@link Slice} of data
     *
     * @see org.springframework.data.jpa.repository.JpaSpecificationExecutor#findAll(org.springframework
     *      .data.jpa.domain.Specification,
     *      org.springframework.data.domain.Pageable)
     */
    public <T, I extends Serializable> Slice<T> findAll(final Specification<T> spec, final Pageable pageable,
            final Class<T> domainClass) {
        final SimpleJpaNoCountRepository<T, I> noCountDao = new SimpleJpaNoCountRepository<>(domainClass, em);
        return noCountDao.findAll(spec, pageable);
    }

    /**
     * Searches without the need for an extra count query.
     *
     * @param pageable
     *            information
     * @param domainClass
     *            of the {@link Entity}
     *
     * @return {@link Slice} of data
     *
     * @see org.springframework.data.jpa.repository.JpaSpecificationExecutor#findAll(org.springframework
     *      .data.jpa.domain.Specification,
     *      org.springframework.data.domain.Pageable)
     */
    public <T, I extends Serializable> Slice<T> findAll(final Pageable pageable, final Class<T> domainClass) {
        final SimpleJpaNoCountRepository<T, I> noCountDao = new SimpleJpaNoCountRepository<>(domainClass, em);
        return noCountDao.findAll(pageable);
    }

    /**
     * Repository implementation with disabled count query.
     * 
     *
     *
     * @param <T>
     *            entity type
     * @param <I>
     *            key or ID type
     */
    public static class SimpleJpaNoCountRepository<T, I extends Serializable> extends SimpleJpaRepository<T, I> {

        /**
         * Constructor.
         *
         * @param domainClass
         *            of the {@link Entity}
         * @param em
         *            {@link EntityManager} instance for the queries
         */
        public SimpleJpaNoCountRepository(final Class<T> domainClass, final EntityManager em) {
            super(domainClass, em);
        }

        @Override
        protected Page<T> readPage(final TypedQuery<T> query, final Pageable pageable, final Specification<T> spec) {
            query.setFirstResult(pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());

            final List<T> content = query.getResultList();

            return new PageImpl<>(content, pageable, content.size());
        }
    }
}
