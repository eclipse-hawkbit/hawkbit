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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import jakarta.transaction.Transactional;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController.Operation;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.DeleteSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.UpdateSpecification;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@Slf4j
public class BaseEntityRepositoryACM<T extends AbstractJpaBaseEntity> implements BaseEntityRepository<T> {

    private static final String SPEC_MUST_NOT_BE_NULL = "Specification must not be null";
    private static final String APPENDED_ACCESS_RULES_SPEC_OF_NON_NULL_SPEC_MUST_NOT_BE_NULL =
            "Appended access rules specification of non-null specification must not be null";

    private final BaseEntityRepository<T> repository;
    private final AccessController<T> accessController;

    BaseEntityRepositoryACM(final BaseEntityRepository<T> repository, final AccessController<T> accessController) {
        this.repository = repository;
        this.accessController = accessController;
    }

    @Override
    @NonNull
    public <S extends T> S save(@NonNull final S entity) {
        accessController.assertOperationAllowed(Operation.UPDATE, entity);
        return repository.save(entity);
    }

    // override because the default implementation is not protected by ACM proxy
    @Override
    public T getById(final Long id) {
        return findOne(byIdSpec(id)).orElseThrow(() -> new EntityNotFoundException(getManagementClass(), id));
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
    public long count() {
        return count((Specification<T>) null);
    }

    @Override
    public void deleteById(@NonNull final Long id) {
        if (!exists(Operation.READ, byIdSpec(id))) {
            throw new EntityNotFoundException(repository.getDomainClass(), id);
        }
        if (!exists(Operation.DELETE, byIdSpec(id))) {
            throw new InsufficientPermissionException();
        }
        repository.deleteById(id);
    }

    @Override
    public void delete(@NonNull final T entity) {
        accessController.assertOperationAllowed(Operation.DELETE, entity);
        repository.delete(entity);
    }

    @Override
    public void deleteAllById(@NonNull final Iterable<? extends Long> ids) {
        final Set<Long> idList = new HashSet<>();
        ids.forEach(idList::add);
        if (count(Operation.DELETE, byIdsSpec(idList)) != idList.size()) {
            throw new InsufficientPermissionException("Has at least one id that is not allowed for deletion!");
        }
        repository.deleteAllById(idList);
    }

    @Override
    public void deleteAll(@NonNull final Iterable<? extends T> entities) {
        accessController.assertOperationAllowed(Operation.DELETE, entities);
        repository.deleteAll(entities);
    }

    @Override
    public void deleteAll() {
        // TODO - shall we remove deleteByTenant and implement this method instead?
//        if (accessController.getAccessRules(AccessController.Operation.DELETE).isPresent()) {
//            throw new InsufficientPermissionException(
//                    "DELETE operation has restriction for given context! deleteAll can't be executed!");
//        }
//        repository.deleteAll();
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends T> List<S> saveAll(final Iterable<S> entities) {
        accessController.assertOperationAllowed(Operation.UPDATE, entities);
        return repository.saveAll(entities);
    }

    @Override
    @NonNull
    public List<T> findAll() {
        return findAll((Specification<T>) null);
    }

    @Override
    @NonNull
    public List<T> findAllById(@NonNull final Iterable<Long> ids) {
        return findAll(byIdsSpec(ids));
    }

    @Override
    public void deleteByTenant(final String tenant) {
        if (accessController.getAccessRules(Operation.DELETE).isPresent()) {
            throw new InsufficientPermissionException("DELETE operation has restriction for given context! deleteAll can't be executed!");
        }
        repository.deleteByTenant(tenant);
    }

    @Override
    public Optional<AccessController<T>> getAccessController() {
        return Optional.of(accessController);
    }

    @Override
    @NonNull
    public Optional<T> findOne(final Specification<T> spec) {
        Objects.requireNonNull(spec, SPEC_MUST_NOT_BE_NULL);
        return repository.findOne(
                // spec shall be non-null and the result of appending rules shall be non-null
                Objects.requireNonNull(
                        accessController.appendAccessRules(Operation.READ, spec),
                        APPENDED_ACCESS_RULES_SPEC_OF_NON_NULL_SPEC_MUST_NOT_BE_NULL));
    }

    @Override
    @NonNull
    public List<T> findAll(final Specification<T> spec) {
        return repository.findAll(accessController.appendAccessRules(Operation.READ, spec));
    }

    @Override
    @NonNull
    public Page<T> findAll(final Specification<T> spec, @NonNull final Pageable pageable) {
        return repository.findAll(accessController.appendAccessRules(Operation.READ, spec), pageable);
    }

    @Override
    public Page<T> findAll(final Specification<T> spec, final Specification<T> countSpec, final Pageable pageable) {
        return repository.findAll(accessController.appendAccessRules(Operation.READ, spec), countSpec, pageable);
    }

    @Override
    @NonNull
    public List<T> findAll(final Specification<T> spec, @NonNull final Sort sort) {
        return repository.findAll(accessController.appendAccessRules(Operation.READ, spec), sort);
    }

    @Override
    public long count(final Specification<T> spec) {
        return repository.count(accessController.appendAccessRules(Operation.READ, spec));
    }

    @Override
    public boolean exists(@NonNull final Specification<T> spec) {
        return repository.exists(
                Objects.requireNonNull(accessController.appendAccessRules(Operation.READ, spec)));
    }

    @Override
    public long update(final UpdateSpecification<T> spec) {
        return repository.update(accessController.appendAccessRules(Operation.UPDATE, spec));
    }

    @Override
    public long delete(final DeleteSpecification<T> spec) {
        return repository.delete(accessController.appendAccessRules(Operation.DELETE, spec));
    }

    @Override
    public <S extends T, R> R findBy(final Specification<T> spec, final Function<? super SpecificationFluentQuery<S>, R> queryFunction) {
        Objects.requireNonNull(spec, SPEC_MUST_NOT_BE_NULL);
        return repository.findBy(
                // spec shall be non-null and the result of appending rules shall be non-null
                Objects.requireNonNull(
                        accessController.appendAccessRules(Operation.READ, spec),
                        APPENDED_ACCESS_RULES_SPEC_OF_NON_NULL_SPEC_MUST_NOT_BE_NULL),
                queryFunction);
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
    public Slice<T> findAllWithoutCount(final Pageable pageable) {
        return findAllWithoutCount(null, pageable);
    }

    @Override
    public Slice<T> findAllWithoutCount(final Specification<T> spec, final Pageable pageable) {
        return repository.findAllWithoutCount(
                accessController.appendAccessRules(Operation.READ, spec), pageable);
    }

    @Override
    @Transactional
    @NonNull
    public <S extends T> S save(Operation operation, @NonNull final S entity) {
        if (operation != null) {
            accessController.assertOperationAllowed(operation, entity);
        }
        return repository.save(entity);
    }

    @Override
    @Transactional
    public <S extends T> List<S> saveAll(final Operation operation, final Iterable<S> entities) {
        if (operation != null) {
            accessController.assertOperationAllowed(operation, entities);
        }
        return repository.saveAll(entities);
    }

    @NonNull
    public Optional<T> findOne(final Operation operation, @NonNull Specification<T> spec) {
        Objects.requireNonNull(spec, SPEC_MUST_NOT_BE_NULL);
        if (operation == null) {
            return repository.findOne(spec);
        } else {
            return repository.findOne(
                    // spec shall be non-null and the result of appending rules shall be non-null
                    Objects.requireNonNull(
                            accessController.appendAccessRules(operation, spec),
                            APPENDED_ACCESS_RULES_SPEC_OF_NON_NULL_SPEC_MUST_NOT_BE_NULL));
        }
    }

    @Override
    @NonNull
    public List<T> findAll(final Operation operation, @Nullable final Specification<T> spec) {
        if (operation == null) {
            return repository.findAll(spec);
        } else {
            return repository.findAll(accessController.appendAccessRules(operation, spec));
        }
    }

    @Override
    @NonNull
    public boolean exists(final Operation operation, Specification<T> spec) {
        if (operation == null) {
            return repository.exists(spec);
        } else {
            return repository.exists(
                    Objects.requireNonNull(accessController.appendAccessRules(operation, spec)));
        }
    }

    @Override
    public long count(final Operation operation, @Nullable final Specification<T> spec) {
        if (operation == null) {
            return repository.count(spec);
        } else {
            return repository.count(accessController.appendAccessRules(operation, spec));
        }
    }

    @Override
    @NonNull
    public Slice<T> findAllWithoutCount(final Operation operation, @Nullable Specification<T> spec, Pageable pageable) {
        if (operation == null) {
            return repository.findAllWithoutCount(spec, pageable);
        } else {
            return repository.findAllWithoutCount(accessController.appendAccessRules(operation, spec), pageable);
        }
    }

    @Override
    @NonNull
    public Class<T> getDomainClass() {
        return repository.getDomainClass();
    }

    @Override
    public Optional<T> findOne(final Specification<T> spec, final String entityGraph) {
        return repository.findOne(
                accessController.appendAccessRules(Operation.READ, spec), entityGraph);
    }

    @Override
    public List<T> findAll(final Specification<T> spec, final String entityGraph) {
        return repository.findAll(
                accessController.appendAccessRules(Operation.READ, spec), entityGraph);
    }

    @Override
    public Page<T> findAll(final Specification<T> spec, final String entityGraph, final Pageable pageable) {
        return repository.findAll(
                accessController.appendAccessRules(Operation.READ, spec), entityGraph, pageable);
    }

    @Override
    public List<T> findAll(final Specification<T> spec, final String entityGraph, final Sort sort) {
        return repository.findAll(
                accessController.appendAccessRules(Operation.READ, spec), entityGraph, sort);
    }

    @SuppressWarnings("unchecked")
    static <T extends AbstractJpaBaseEntity, R extends BaseEntityRepository<T>> R of(
            final R repository, @NonNull final AccessController<T> accessController) {
        Objects.requireNonNull(repository);
        Objects.requireNonNull(accessController);
        final BaseEntityRepositoryACM<T> repositoryACM = new BaseEntityRepositoryACM<>(repository, accessController);
        final R acmProxy = (R) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                repository.getClass().getInterfaces(),
                (proxy, method, args) -> {
                    try {
                        // try to call a BaseEntityRepositoryACM method if declared
                        // TODO - cache mapping so to speed things
                        final Method delegateMethod =
                                BaseEntityRepositoryACM.class.getDeclaredMethod(method.getName(), method.getParameterTypes());
                        return delegateMethod.invoke(repositoryACM, args);
                    } catch (final InvocationTargetException e) {
                        throw e.getCause() == null ? e : e.getCause();
                    } catch (final NoSuchMethodException nsme) {
                        // not found
                        try {
                            // call to call a repository method
                            if (method.getName().startsWith("find") || method.getName().startsWith("get")) {
                                final Object result = method.invoke(repository, args);
                                // Iterable, List, Page, Slice
                                if (Iterable.class.isAssignableFrom(method.getReturnType())) {
                                    for (final Object e : (Iterable<?>) result) {
                                        if (repository.getDomainClass().isAssignableFrom(e.getClass())) {
                                            accessController.assertOperationAllowed(Operation.READ, (T) e);
                                        }
                                    }
                                } else if (Optional.class.isAssignableFrom(method.getReturnType()) && ((Optional<?>) result)
                                        .filter(value -> repository.getDomainClass().isAssignableFrom(value.getClass()))
                                        .isPresent()) {
                                    return ((Optional<T>) result).filter(
                                            t -> {
                                                // if not accessible - throws exception (as for iterables or single entities)
                                                accessController.assertOperationAllowed(Operation.READ, t);
                                                return true;
                                            });
                                } else if (repository.getDomainClass().isAssignableFrom(method.getReturnType())) {
                                    accessController.assertOperationAllowed(Operation.READ, (T) result);
                                }
                                return result;
                            } else if ("toString".equals(method.getName()) && method.getParameterCount() == 0) {
                                return BaseEntityRepositoryACM.class.getSimpleName() +
                                        "(repository: " + repository + ", accessController: " + accessController + ")";
                            } else {
                                return method.invoke(repository, args);
                            }
                        } catch (final InvocationTargetException ite) {
                            throw ite.getCause() == null ? ite : ite.getCause();
                        }
                    }
                });
        log.info("Proxy created -> {}", acmProxy);
        return acmProxy;
    }
}
