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

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.RepositoryManagement;
import org.eclipse.hawkbit.repository.RsqlQueryField;
import org.eclipse.hawkbit.repository.builder.Utils;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.repository.BaseEntityRepository;
import org.eclipse.hawkbit.repository.jpa.rsql.RsqlUtility;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link DistributionSetManagement}.
 */
@Transactional(readOnly = true)
@Validated
abstract class AbstractJpaRepositoryManagement<T extends AbstractJpaBaseEntity, C extends RepositoryManagement.Builder<T>, U extends Identifiable<Long>,
        R extends BaseEntityRepository<T>, A extends Enum<A> & RsqlQueryField>  // J is the JPA entity type, A is the RSQL query field enum
        implements RepositoryManagement<T, C, U> {

    public static final String DELETED = "deleted";

    protected final R jpaRepository;
    protected final EntityManager entityManager;

    protected AbstractJpaRepositoryManagement(final R jpaRepository, final EntityManager entityManager) {
        this.jpaRepository = jpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public T create(final C create) {
        return jpaRepository.save(AccessController.Operation.CREATE, create.build());
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public List<T> create(final Collection<C> create) {
        return Collections.unmodifiableList(jpaRepository.saveAll(
                AccessController.Operation.CREATE,
                create.stream().map(Builder::build).toList()));
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    @SuppressWarnings("java:S1066") // javaS1066 - better readable that way
    public T update(final U update) {
        final T entity = jpaRepository.findById(update.getId()).orElseThrow(() -> new EntityNotFoundException(managementClass(), update.getId()));
        return update(update, entity);
    }

    protected T update(final U update, final T entity) {
        if (Utils.update(update, entity)) {
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

    @Override
    public Optional<T> get(final long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<T> get(final Collection<Long> ids) {
        return Collections.unmodifiableList(findAllById(ids));
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
    public Slice<T> findAll(final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(
                jpaRepository,
                isNotDeleted().map(List::of).orElseGet(Collections::emptyList),
                pageable);
    }

    @Override
    public Page<T> findByRsql(final String rsql, final Pageable pageable) {
        return JpaManagementHelper.findAllWithCountBySpec(
                jpaRepository,
                isNotDeleted()
                        .map(isNotDeleted ->
                                List.of(RsqlUtility.getInstance().<A, T> buildRsqlSpecification(rsql, fieldsClass()), isNotDeleted))
                        .orElseGet(() -> List.of(RsqlUtility.getInstance().buildRsqlSpecification(rsql, fieldsClass()))),
                pageable);
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
}