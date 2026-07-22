/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.repository;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command repository operations for all {@link TenantAwareBaseEntity}s.
 * <p>
 * When accessed through the wrapper from {@link #withWrapper(AccessController)}, {@link #findById} is served
 * from the by-id cache (see {@link #getCache()}) and access control is re-checked in-memory on every call.
 *
 * @param <T> the entity type
 */
@NoRepositoryBean
@Transactional(readOnly = true)
public interface BaseEntityRepository<T extends AbstractJpaBaseEntity>
        extends PagingAndSortingRepository<T, Long>, CrudRepository<T, Long>, JpaSpecificationExecutor<T>,
        JpaSpecificationEntityGraphExecutor<T>, NoCountSliceRepository<T>, ACMRepository<T> {

    /**
     * Loads an entity by id directly from the database, bypassing the by-id cache. Use {@link #findById}
     * for the cached, access-controlled lookup.
     *
     * @param id the entity id
     * @return the matching entity
     * @throws EntityNotFoundException if no entity with the given id exists
     */
    default T getById(final Long id) {
        return findOne(byIdSpec(id)).orElseThrow(() -> new EntityNotFoundException(getManagementClass(), id));
    }

    /**
     * Overrides {@link org.springframework.data.repository.CrudRepository#saveAll(Iterable)} to return a list of created entities instead
     * of an instance of {@link Iterable} to be able to work with it directly in further code processing instead of converting the
     * {@link Iterable}.
     *
     * @param entities to persist in the database
     * @return the created entities
     */
    @Override
    @Transactional
    <S extends T> List<S> saveAll(Iterable<S> entities);

    /**
     * Overrides {@link org.springframework.data.repository.CrudRepository#findAll()} to return a list of found entities instead of
     * an instance of {@link Iterable} to be able to work with it directly in further code processing instead of converting the
     * {@link Iterable}.
     *
     * @return the found entities
     */
    @Override
    List<T> findAll();

    /**
     * Overrides {@link org.springframework.data.repository.CrudRepository#findAllById(Iterable)} to return a list of found entities instead
     * of an instance of {@link Iterable} to be able to work with it directly in further code processing instead of converting the
     * {@link Iterable}.
     *
     * @param ids to search in the database for
     * @return the found entities
     */
    @Override
    List<T> findAllById(final Iterable<Long> ids);

    // TODO To be considered if this method is needed at all

    /**
     * Deletes all entities of a given tenant from this repository. For safety reasons (this is a "delete everything" query after all) we add
     * the tenant manually to query even if this will be done by {@link EntityManager} anyhow. The DB should take care of optimizing this away.
     * <p/>
     *
     * @param tenant to delete data from
     */
    void deleteByTenant(String tenant);

    /**
     * Wraps this repository so that {@link #findById} is served from the by-id cache (see {@link #getCache()})
     * and, when an {@link AccessController} is given, access control is enforced below the management layer.
     * <p>
     * The wrapper resolves {@link #getCache()} lazily on each call, so it is safe to create here during bean
     * initialization - before the tenant-aware cache manager is ready.
     *
     * @param accessController the access controller to enforce, or {@code null} to skip access control
     * @return the wrapping repository, or {@code this} when there is nothing to add (no access controller and no cache)
     */
    default BaseEntityRepository<T> withWrapper(@Nullable final AccessController<T> accessController) {
        // Wrap only when there is something to add: access control, or a by-id cache. isCached() is used
        // instead of getCache() because decoration happens at bean-post-processing time, before the tenant
        // cache manager is guaranteed initialized; the facade still resolves getCache() lazily per call.
        return (accessController == null && !isCached()) ? this : BaseEntityRepositoryWrapper.of(this, accessController);
    }

    /**
     * Whether this repository caches {@link #findById} results by id.
     * <p>
     * Cheap and startup-safe, so it can be called while wiring beans - unlike {@link #getCache()}, which resolves the
     * tenant-aware cache manager and must only be called at runtime. Cached repositories override this to {@code true}.
     *
     * @return {@code true} if by-id results are cached; {@code false} by default
     */
    default boolean isCached() {
        return false;
    }

    /**
     * The cache holding raw, permission-agnostic entities keyed by id for {@link #findById}.
     * <p>
     * Access control is not baked into the cached value - it is re-checked in-memory on each call - so a single entry
     * is safely shared across principals. Repositories that opt into caching override this; the default is no cache.
     *
     * @return the by-id entity cache, or {@link Optional#empty()} when caching is disabled
     */
    default Optional<Cache> getCache() {
        return Optional.empty();
    }

    default <S extends AbstractJpaBaseEntity> Specification<S> byIdSpec(final Long id) {
        return (root, query, cb) -> cb.equal(root.get(AbstractJpaBaseEntity_.id), id);
    }

    default <S extends AbstractJpaBaseEntity> Specification<S> byIdsSpec(final Iterable<Long> ids) {
        final Collection<Long> collection;
        if (ids instanceof Collection<Long> idCollection) {
            collection = idCollection;
        } else {
            collection = new LinkedList<>();
            ids.forEach(collection::add);
        }
        return (root, query, cb) -> root.get(AbstractJpaBaseEntity_.id).in(collection);
    }

    default Optional<AccessController<T>> getAccessController() {
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    default Class<? extends BaseEntity> getManagementClass() {
        final Class<T> domainClass = getDomainClass();
        final String domainClassSimpleName = domainClass.getSimpleName();
        if (!domainClassSimpleName.toLowerCase().startsWith("jpa")) {
            return domainClass;
        }
        final String managementClassSimpleName = domainClassSimpleName.substring(3);
        final Class<?> superClass = domainClass.getSuperclass();
        if (superClass != null &&
                superClass.getSimpleName().equals(managementClassSimpleName) && BaseEntity.class.isAssignableFrom(superClass)) {
            return (Class<? extends BaseEntity>) superClass;
        }

        for (final Class<?> interfaceClass : domainClass.getInterfaces()) {
            if (interfaceClass.getSimpleName().equals(managementClassSimpleName) && BaseEntity.class.isAssignableFrom(interfaceClass)) {
                return (Class<? extends BaseEntity>) interfaceClass;
            }
        }
        return domainClass;
    }
}