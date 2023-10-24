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

import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaTenantAwareBaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.persistence.criteria.CriteriaBuilder;
import javax.transaction.Transactional;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class BaseEntityRepositoryACM<T extends AbstractJpaTenantAwareBaseEntity> implements BaseEntityRepository<T> {

    private final BaseEntityRepository<T> repository;
    private final Class<T> entityType;
    private final AccessController<T> accessController;

    protected BaseEntityRepositoryACM(final BaseEntityRepository<T> repository, final Class<T> entityType, final AccessController<T> accessController) {
        this.repository = repository;
        this.entityType = entityType;
        this.accessController = accessController;
    }

    @SuppressWarnings("unchecked")
    public static <T extends AbstractJpaTenantAwareBaseEntity, R extends BaseEntityRepository<T>> R of(
            final R repository, final Class<T> entityType, final AccessController<T> accessController) {
        if (accessController == null) {
            return repository;
        } else {
            final BaseEntityRepositoryACM<T> repositoryACM =
                    new BaseEntityRepositoryACM<>(repository, entityType, accessController);
            return (R) Proxy.newProxyInstance(
                    Thread.currentThread().getContextClassLoader(),
                    repository.getClass().getInterfaces(),
                    (proxy, method, args) -> {
                        try {
                            try {
                                // TODO - cache mapping so to speed things
                                final Method delegateMethod =
                                        BaseEntityRepositoryACM.class.getDeclaredMethod(
                                                method.getName(), method.getParameterTypes());
                                return delegateMethod.invoke(repositoryACM, args);
                            } catch (final NoSuchMethodException e) {
                                // call to repository itself
                            }
                            if (method.getName().startsWith("find")) {
                                final Object result = method.invoke(repository, args);
                                if (Iterable.class.isAssignableFrom(method.getReturnType())) {
                                    for (final T e : ((Iterable<T>) result)) {
                                        accessController.assertOperationAllowed(AccessController.Operation.READ, e);
                                    }
                                } else if (Page.class.isAssignableFrom(method.getReturnType()) || Slice.class.isAssignableFrom(method.getReturnType())) {
                                    return result;
                                } else if (Optional.class.isAssignableFrom(method.getReturnType())) {
                                    return ((Optional<T>)result).map(t -> isOperationAllowed(AccessController.Operation.READ, t, accessController));
                                } else {
                                    accessController.assertOperationAllowed(AccessController.Operation.READ, (T)result);
                                }
                                return result;
                            } else {
                                return method.invoke(repository, args);
                            }
                        } catch (final InvocationTargetException e) {
                            throw e.getCause() == null ? e : e.getCause();
                        }
                    });
        }
    }

    @Override
    @NonNull
    public Optional<T> findById(@NonNull final Long id) {
        return findOne(byIdSpec(id));
    }

    @Override
    public boolean existsById(@NonNull final Long id) {
        return exists(byIdSpec(id));
    }

    @Override
    @NonNull
    public Iterable<T> findAll() {
        return findAll((Specification<T>) null);
    }

    @Override
    @NonNull
    public List<T> findAllById(@NonNull final Iterable<Long> ids) {
        // TODO AC - should we throw exception if not all are found?
        return findAll(byIdsSpec(ids));
    }

    @Override
    public long count() {
        return count(null);
    }

    @Override
    public void deleteById(@NonNull final Long id) {
        if (!exists(AccessController.Operation.DELETE, byIdSpec(id))) {
          throw new EntityNotFoundException(entityType, id);
        }
        repository.deleteById(id);
    }

    @Override
    public void delete(@NonNull final T entity) {
        accessController.assertOperationAllowed(AccessController.Operation.DELETE, entity);
        repository.delete(entity);
    }

    @Override
    public void deleteAllById(@NonNull final Iterable<? extends Long> ids) {
        final List<Long> idList = toList(ids).stream().map(e -> (Long)e).toList();
        if (count(AccessController.Operation.DELETE, byIdsSpec(idList)) != idList.size()) {
            throw new InsufficientPermissionException("Has id that doesn't exist or is not allowed for deletion!");
        }
        repository.deleteAllById(ids);
    }

    @Override
    public void deleteAll(@NonNull final Iterable<? extends T> entities) {
        accessController.assertOperationAllowed(AccessController.Operation.DELETE, entities);
        repository.deleteAll(entities);
    }

    @Override
    public void deleteAll() {
        // TODO AC - shall this method throw exception having that we have deleteByTenant
        // in order to do not allow deletion for all tenants?
        if (accessController.getAccessRules(AccessController.Operation.DELETE).isPresent()) {
            throw new InsufficientPermissionException(
                    "DELETE operation has restriction for given context! deleteAll can't be executed!");
        }
        repository.deleteAll();
    }

    @Override
    @NonNull
    public <S extends T> S save(@NonNull final S entity) {
        accessController.assertOperationAllowed(AccessController.Operation.UPDATE, entity);
        return repository.save(entity);
    }

    @Override
    public <S extends T> List<S> saveAll(final Iterable<S> entities) {
        accessController.assertOperationAllowed(AccessController.Operation.UPDATE, entities);
        return repository.saveAll(entities);
    }

    @Override
    @NonNull
    public Optional<T> findOne(final Specification<T> spec) {
        return repository.findOne(accessController.appendAccessRules(AccessController.Operation.READ, spec));
    }

    @Override
    @NonNull
    public Iterable<T> findAll(@NonNull final Sort sort) {
        return findAll(null, sort);
    }

    @Override
    @NonNull
    public Page<T> findAll(@NonNull final Pageable pageable) {
        return findAll(null, pageable);
    }

    @Override
    @NonNull
    public List<T> findAll(final Specification<T> spec) {
        return repository.findAll(accessController.appendAccessRules(AccessController.Operation.READ, spec));
    }

    @Override
    @NonNull
    public Page<T> findAll(final Specification<T> spec, @NonNull final Pageable pageable) {
        return repository.findAll(accessController.appendAccessRules(AccessController.Operation.READ, spec), pageable);
    }

    @Override
    @NonNull
    public List<T> findAll(final Specification<T> spec, @NonNull final Sort sort) {
        return repository.findAll(accessController.appendAccessRules(AccessController.Operation.READ, spec), sort);
    }

    @Override
    public long count(final Specification<T> spec) {
        return repository.count(accessController.appendAccessRules(AccessController.Operation.READ, spec));
    }

    @Override
    public boolean exists(@NonNull final Specification<T> spec) {
        return repository.exists(
                Objects.requireNonNull(accessController.appendAccessRules(AccessController.Operation.READ, spec)));
    }

    @Override
    public Slice<T> findAllWithoutCount(final Pageable pageable) {
        return findAllWithoutCount(null, pageable);
    }

    @Override
    public Slice<T> findAllWithoutCount(final Specification<T> spec, final Pageable pageable) {
        return repository.findAllWithoutCount(
                accessController.appendAccessRules(AccessController.Operation.READ, spec), pageable);
    }

    @Override
    @Transactional
    @NonNull
    public <S  extends T> S save(@NonNull AccessController.Operation operation, @NonNull final S entity) {
        accessController.assertOperationAllowed(operation, entity);
        return repository.save(entity);
    }

    @Override
    @Transactional
    public <S extends T> List<S> saveAll(@NonNull AccessController.Operation operation, final Iterable<S> entities) {
        accessController.assertOperationAllowed(operation, entities);
        return repository.saveAll(entities);
    }

    @NonNull
    public Optional<T> findOne(@NonNull AccessController.Operation operation, @Nullable Specification<T> spec) {
        return repository.findOne(accessController.appendAccessRules(operation, spec));
    }

    @Override
    @NonNull
    public List<T> findAll(@NonNull final AccessController.Operation operation, @Nullable final Specification<T> spec) {
        return repository.findAll(accessController.appendAccessRules(operation, spec));
    }

    @Override
    @NonNull
    public boolean exists(@NonNull AccessController.Operation operation, Specification<T> spec) {
        return repository.exists(
                Objects.requireNonNull(accessController.appendAccessRules(operation, spec)));
    }

    @Override
    @NonNull
    public long count(@NonNull final AccessController.Operation operation, @Nullable final Specification<T> spec) {
        return repository.count(accessController.appendAccessRules(operation, spec));
    }

    @Override
    @NonNull
    public Slice<T> findAllWithoutCount(
            @NonNull final AccessController.Operation operation, @Nullable Specification<T> spec, Pageable pageable) {
        return repository.findAllWithoutCount(accessController.appendAccessRules(operation, spec),pageable);
    }

    @Override
    public void deleteByTenant(final String tenant) {
        if (accessController.getAccessRules(AccessController.Operation.DELETE).isPresent()) {
            throw new InsufficientPermissionException(
                    "DELETE operation has restriction for given context! deleteAll can't be executed!");
        }
        repository.deleteByTenant(tenant);
    }

    private static <T extends AbstractJpaTenantAwareBaseEntity> Specification<T> byIdSpec(final Long id) {
        return (root, query, cb) -> cb.equal(root.get(AbstractJpaBaseEntity_.id), id);
    }

    private static <T extends AbstractJpaTenantAwareBaseEntity> Specification<T> byIdsSpec(final Iterable<Long> ids) {
        return (root, query, cb) -> {
            final CriteriaBuilder.In<Long> in = cb.in(root.get(AbstractJpaBaseEntity_.id));
            ids.forEach(in::value);
            return in;
        };
    }

    private static <T> boolean isOperationAllowed(
            final AccessController.Operation operation, T entity,
            final AccessController<T> accessController) {
        try {
            accessController.assertOperationAllowed(operation, entity);
            return true;
        } catch (final InsufficientPermissionException e) {
            return false;
        }
    }

    private static <T> List<T> toList(final Iterable<T> i) {
        if (i instanceof List<T> l) {
            return l;
        } else {
            final List<T> l = new ArrayList<>();
            i.forEach(l::add);
            return l;
        }
    }
}
