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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command repository operations for all {@link TenantAwareBaseEntity}s.
 * @param <T> type if the entity type
 */
@NoRepositoryBean
@Transactional(readOnly = true)
public interface BaseEntityRepository<T extends AbstractJpaBaseEntity>
        extends PagingAndSortingRepository<T, Long>, CrudRepository<T, Long>, JpaSpecificationExecutor<T>,
        JpaSpecificationEntityGraphExecutor<T>, NoCountSliceRepository<T>, ACMRepository<T> {

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
     * Returns a wrapper (or the same instance if access controller is <code>null</code> of this repository that supports ACM.
     * <p/>
     * Note: To use ACM support the returned object shall be used! <code>this</code> object will not achieve ACM support!
     * <p/>
     * Notes on ACM support (if enabled, i.e. <code>accessController</code> is not <code>null</code>):
     * <ul>
     *     <li>ACM is applied for all {@link BaseEntityRepository} methods</li>
     *     <li>ACM is applied for all <code>findXXX</code> methods that returns {@link Iterable}
     *         (expected of <code>? extends T</code>),
     *         <code>? extends T</code> or {@link java.util.Optional}(expected of <code>? extends T</code>).
     *     </li>
     *     <li>For all other methods defined on repository interface level that are implemented, for instance,
     *         via {@link org.springframework.data.jpa.repository.Query} or using naming conventions
     *         (existsBySomething) the ACM won't be applied!
     *     </li>
     * </ul>
     *
     * @param accessController access controller to be applied to the result
     * @return a repository that supports ACM.
     */
    default BaseEntityRepository<T> withACM(@Nullable final AccessController<T> accessController) {
        if (accessController == null) {
            return this;
        } else {
            return BaseEntityRepositoryACM.of(this, accessController);
        }
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

    @SuppressWarnings("uchecked")
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