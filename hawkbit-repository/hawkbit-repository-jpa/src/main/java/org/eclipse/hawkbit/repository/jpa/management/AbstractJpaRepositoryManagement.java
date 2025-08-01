/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.eclipse.hawkbit.repository.jpa.configuration.Constants.TX_RT_DELAY;
import static org.eclipse.hawkbit.repository.jpa.configuration.Constants.TX_RT_MAX;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.RepositoryManagement;
import org.eclipse.hawkbit.repository.RsqlQueryField;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.Jpa;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.JpaRepositoryConfiguration;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.repository.BaseEntityRepository;
import org.eclipse.hawkbit.repository.jpa.rsql.RsqlUtility;
import org.eclipse.hawkbit.utils.ObjectCopyUtil;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.authorization.method.HandleAuthorizationDenied;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link DistributionSetManagement}.
 *
 * @param <T> the JPA entity type
 * @param <C> the type of the create request
 * @param <U> the type of the update request
 * @param <R> the type of the entity JPA repository
 * @param <A> RSQL query field enum
 */
// Spring AOP doesn't support bridge methods and the AspectJ advices as ExceptionMappingAspectHandler could not handle the
// thrown exception (e.g. to convert AuthorizationDeniedException to InsufficientPermissionException).
// That's why we explictly handle the insufficient permission exception with this @HandleAuthorizationDenied annotation.
@HandleAuthorizationDenied(handlerClass = JpaRepositoryConfiguration.ManagementExceptionThrowingMethodAuthorizationDeniedHandler.class)
@Transactional(readOnly = true)
@Validated
abstract class AbstractJpaRepositoryManagement<T extends AbstractJpaBaseEntity, C, U extends Identifiable<Long>, R extends BaseEntityRepository<T>, A extends Enum<A> & RsqlQueryField>
        implements RepositoryManagement<T, C, U> {

    public static final String DELETED = "deleted";

    protected final R jpaRepository;
    protected final EntityManager entityManager;
    private final Supplier<T> jpaEntityCreator;

    protected AbstractJpaRepositoryManagement(final R jpaRepository, final EntityManager entityManager) {
        this.jpaRepository = jpaRepository;
        this.entityManager = entityManager;
        final Constructor<T> entityConstructor;
        try {
            entityConstructor = jpaRepository.getDomainClass().getConstructor();
            // test if method works, if fine - it shall never fail when called later
            entityConstructor.newInstance();
        } catch (final NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("JPA domain class " + jpaRepository.getDomainClass() + " shall have public no-args constructor", e);
        }
        jpaEntityCreator = () -> {
            try {
                return entityConstructor.newInstance();
            } catch (final InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Must NEVER happen!", e);
            }
        };
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public T create(final C create) {
        return jpaRepository.save(AccessController.Operation.CREATE, jpaEntity(create));
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public List<T> create(final Collection<C> create) {
        return jpaRepository.saveAll(AccessController.Operation.CREATE, create.stream().map(this::jpaEntity).toList());
    }

    @Override
    public Optional<T> get(final long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<T> get(final Collection<Long> ids) {
        return findAllById(ids);
    }

    @Override
    public boolean exists(final long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public long count() {
        return jpaRepository.count(isNotDeleted().orElse(null));
    }

    @Override
    public long countByRsql(String rsql) {
        return jpaRepository.count(JpaManagementHelper.combineWithAnd(rsqlSpec(rsql)));
    }

    @Override
    public Slice<T> findAll(final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(
                jpaRepository, isNotDeleted().map(List::of).orElseGet(Collections::emptyList), pageable);
    }

    @Override
    public Page<T> findByRsql(final String rsql, final Pageable pageable) {
        return JpaManagementHelper.findAllWithCountBySpec(jpaRepository, rsqlSpec(rsql), pageable);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    @SuppressWarnings("java:S1066") // javaS1066 - better readable that way
    public T update(final U update) {
        final T entity = jpaRepository
                .findById(update.getId())
                .orElseThrow(() -> new EntityNotFoundException(managementClass(), update.getId()));
        return update(update, entity);
    }

    protected T update(final Identifiable<Long> update, final T entity) {
        // update getId has no setter in target JPA entity but shall have getter and the value shall be the same
        // otherwise the Utils will throw an exception that there is no counterpart setter for getId
        if (ObjectCopyUtil.copy(update, entity, false, this::attach)) {
            return jpaRepository.save(entity);
        } else { // otherwise it is not changed, so return the same entity
            return entity;
        }
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public void delete(final long id) {
        delete0(List.of(id));
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public void delete(final Collection<Long> ids) {
        delete0(ids);
    }

    @NotNull
    private List<Specification<T>> rsqlSpec(final String rsql) {
        return isNotDeleted()
                .map(isNotDeleted -> List.of(RsqlUtility.getInstance().<A, T> buildRsqlSpecification(rsql, fieldsClass()), isNotDeleted))
                .orElseGet(() -> List.of(RsqlUtility.getInstance().buildRsqlSpecification(rsql, fieldsClass())));
    }

    // return which are for soft deletion
    @SuppressWarnings("java:S1172") // java:S1172 - it is intended to be used by subclasses
    protected Collection<T> softDelete(final Collection<T> toDelete) {
        return Collections.emptyList();
    }

    protected void delete0(final Collection<Long> ids) {
        if (ObjectUtils.isEmpty(ids)) {
            return;
        }

        final List<T> toDelete = findAllById(ids); // throws EntityNotFoundException if any of these does not exist
        jpaRepository.getAccessController().ifPresent(ac -> {
            for (final T entity : toDelete) {
                ac.assertOperationAllowed(AccessController.Operation.DELETE, entity);
            }
        });

        // mark the rest as hard delete
        final Collection<Long> toSoftDelete = softDelete(toDelete).stream().map(T::getId).toList();
        if (!toSoftDelete.isEmpty()) {
            if (!supportSoftDelete()) {
                throw new IllegalStateException("Soft delete is not supported for " + jpaRepository.getDomainClass().getSimpleName());
            }

            final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            final CriteriaUpdate<T> update = cb.createCriteriaUpdate(jpaRepository.getDomainClass());
            final Root<T> root = update.from(jpaRepository.getDomainClass());
            final CriteriaBuilder.In<Long> in = cb.in(root.get(AbstractJpaBaseEntity_.id));
            toSoftDelete.forEach(in::value);
            update.set(root.get(DELETED), true).where(in);
            entityManager.createQuery(update).executeUpdate();
        }

        final List<Long> toHardDelete = toDelete.stream()
                .map(AbstractJpaBaseEntity::getId)
                .filter(id -> !toSoftDelete.contains(id))
                .toList();
        // hard delete the rest if exists
        if (!toHardDelete.isEmpty()) {
            // don't give the delete statement an empty list, JPA/Oracle cannot
            // handle the empty list
            jpaRepository.deleteAllById(toHardDelete);
        }
    }

    private List<T> findAllById(final Collection<Long> ids) {
        final List<T> foundDs = jpaRepository.findAllById(ids);
        if (foundDs.size() != ids.size()) {
            throw new EntityNotFoundException(managementClass(), ids, foundDs.stream().map(T::getId).toList());
        }
        return foundDs;
    }

    private final Supplier<Class<T>> managementClassSupplier = SingletonSupplier.of(this::managementClass0);

    private Class<T> managementClass() {
        return managementClassSupplier.get();
    }

    @SuppressWarnings("unchecked")
    private Class<T> managementClass0() {
        return (Class<T>) jpaRepository.getManagementClass();
    }

    private final Supplier<Class<A>> fieldsClassSupplier = SingletonSupplier.of(this::fieldsClass0);

    private Class<A> fieldsClass() {
        return fieldsClassSupplier.get();
    }

    @SuppressWarnings("unchecked")
    private Class<A> fieldsClass0() {
        try {
            return (Class<A>) Class.forName("org.eclipse.hawkbit.repository." + managementClass().getSimpleName() + "Fields");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private final Supplier<Boolean> supportSoftDeleteSupplier = SingletonSupplier.of(this::supportSoftDelete0);

    private boolean supportSoftDelete() {
        return supportSoftDeleteSupplier.get();
    }

    private boolean supportSoftDelete0() {
        return Stream.of(jpaRepository.getDomainClass().getDeclaredFields()).map(Field::getName).anyMatch(DELETED::equals);
    }

    private final Supplier<Optional<Specification<T>>> isNotDeletedSupplier = SingletonSupplier.of(this::isNotDeleted0);

    private Optional<Specification<T>> isNotDeleted() {
        return isNotDeletedSupplier.get();
    }

    private Optional<Specification<T>> isNotDeleted0() {
        return supportSoftDelete()
                ? Optional.of((root, query, cb) -> cb.equal(root.get(DELETED), false))
                : Optional.empty();
    }

    private T jpaEntity(final Object create) {
        final T jpaEntity = jpaEntityCreator.get();
        ObjectCopyUtil.copy(create, jpaEntity, false, this::attach);
        return jpaEntity;
    }

    private Object attach(final Object propertyValue) {
        if (Jpa.JPA_VENDOR != Jpa.JpaVendor.HIBERNATE) {
            return propertyValue; // no need to attach, only Hibernate supports this
        }

        if (propertyValue instanceof List<?> list) {
            return list.stream().map(this::attach).toList();
        } else if (propertyValue instanceof Set<?> set) {
            return set.stream().map(this::attach).collect(Collectors.toSet());
        } else if (propertyValue instanceof Map<?, ?> map) {
            return map.entrySet().stream().collect(Collectors.toMap(
                    entry -> attach(entry.getKey()),
                    entry -> attach(entry.getValue())));
        } else if (attachable(propertyValue)) {
            // hibernate require detached entities to be attached before setting to jpa entity as a sub-property
            return entityManager.merge(propertyValue);
        } else {
            return propertyValue; // no change
        }
    }

    private boolean attachable(final Object propertyValue) {
        if (propertyValue == null) {
            return false;
        }

        final Class<?> clazz = propertyValue.getClass();
        return !clazz.isPrimitive() && !clazz.isEnum() &&
                clazz != String.class && !Number.class.isAssignableFrom(clazz) && !Boolean.class.isAssignableFrom(clazz) &&
                !entityManager.contains(propertyValue); // no need to attach
    }
}