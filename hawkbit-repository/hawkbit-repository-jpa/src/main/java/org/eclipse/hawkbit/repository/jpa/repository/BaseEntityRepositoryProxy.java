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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import jakarta.transaction.Transactional;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController.Operation;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.DeleteSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.UpdateSpecification;

/**
 * Repository wrapper that sits below the management layer and adds two concerns to the delegated repository:
 * <ul>
 * <li><b>caching</b> - {@link #findById(Long)} loads a raw, permission-agnostic entity through
 * {@link BaseEntityRepository#getCache()} (when the delegate opts into caching);</li>
 * <li><b>access control</b> - when an {@link AccessController} is present, READ is asserted in-memory on the
 * (possibly cached) entity, and the remaining operations append access rules to their queries.</li>
 * </ul>
 * Keeping access control out of the cached value lets a single cache entry be shared across principals while each
 * caller's permissions are still enforced on every call.
 */
@Slf4j
public class BaseEntityRepositoryProxy<T extends AbstractJpaBaseEntity> implements BaseEntityRepository<T> {

    private static final String SPEC_MUST_NOT_BE_NULL = "Specification must not be null";

    private final BaseEntityRepository<T> repository;
    @Nullable
    private final AccessController<T> accessController;

    BaseEntityRepositoryProxy(final BaseEntityRepository<T> repository, @Nullable final AccessController<T> accessController) {
        this.repository = repository;
        this.accessController = accessController;
    }

    /**
     * Loads the entity by id through the cache and, when access control is active, returns it only if the current
     * context is allowed to READ it - otherwise {@link Optional#empty()}.
     */
    @Override
    @NonNull
    public Optional<T> findById(@NonNull final Long id) {
        final Optional<T> entity = loadCached(id);
        return accessController == null ? entity : entity.filter(this::readable);
    }

    /** Serves the entity from {@link BaseEntityRepository#getCache()} when the delegate is cached, else loads it from the delegate. */
    private Optional<T> loadCached(final Long id) {
        final Cache cache = repository.getCache().orElse(null);
        if (cache == null) {
            return repository.findById(id);
        } else {
            // we cache only value - not optionals
            return Optional.ofNullable(cache.get(id, () -> repository.findById(id).orElse(null)));
        }
    }

    @Override
    @NonNull
    public <S extends T> S save(@NonNull final S entity) {
        assertAllowed(Operation.UPDATE, entity);
        return repository.save(entity);
    }

    @Override
    public <S extends T> List<S> saveAll(final Iterable<S> entities) {
        assertAllowed(Operation.UPDATE, entities);
        return repository.saveAll(entities);
    }

    @Override
    @Transactional
    @NonNull
    public <S extends T> S save(final Operation operation, @NonNull final S entity) {
        assertAllowed(operation, entity);
        return repository.save(entity);
    }

    @Override
    @Transactional
    public <S extends T> List<S> saveAll(final Operation operation, final Iterable<S> entities) {
        assertAllowed(operation, entities);
        return repository.saveAll(entities);
    }

    @Override
    public T getById(final Long id) {
        return findOne(byIdSpec(id)).orElseThrow(() -> new EntityNotFoundException(getManagementClass(), id));
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
        assertAllowed(Operation.DELETE, entity);
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
        assertAllowed(Operation.DELETE, entities);
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
    public void deleteByTenant(final String tenant) {
        if (accessController != null && accessController.getAccessRules(Operation.DELETE).isPresent()) {
            throw new InsufficientPermissionException("DELETE operation has restriction for given context! deleteAll can't be executed!");
        }
        repository.deleteByTenant(tenant);
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
    public Optional<AccessController<T>> getAccessController() {
        return Optional.ofNullable(accessController);
    }

    @Override
    @NonNull
    public Optional<T> findOne(final Specification<T> spec) {
        Objects.requireNonNull(spec, SPEC_MUST_NOT_BE_NULL);
        return repository.findOne(appendRules(Operation.READ, spec));
    }

    @Override
    @NonNull
    public List<T> findAll(final Specification<T> spec) {
        return repository.findAll(appendRules(Operation.READ, spec));
    }

    @Override
    @NonNull
    public Page<T> findAll(@Nullable final Specification<T> spec, @NonNull final Pageable pageable) {
        return repository.findAll(appendRules(Operation.READ, spec), pageable);
    }

    @Override
    public Page<T> findAll(@Nullable final Specification<T> spec, @NonNull final Specification<T> countSpec, @NonNull final Pageable pageable) {
        return repository.findAll(appendRules(Operation.READ, spec), countSpec, pageable);
    }

    @Override
    @NonNull
    public List<T> findAll(@Nullable final Specification<T> spec, @NonNull final Sort sort) {
        return repository.findAll(appendRules(Operation.READ, spec), sort);
    }

    @Override
    public long count(@Nullable final Specification<T> spec) {
        return repository.count(appendRules(Operation.READ, spec));
    }

    @Override
    public boolean exists(@NonNull final Specification<T> spec) {
        Objects.requireNonNull(spec, SPEC_MUST_NOT_BE_NULL);
        return repository.exists(appendRules(Operation.READ, spec));
    }

    @Override
    public long update(@Nullable final UpdateSpecification<T> spec) {
        return repository.update(accessController == null ? spec : accessController.appendAccessRules(Operation.UPDATE, spec));
    }

    @Override
    public long delete(@Nullable final DeleteSpecification<T> spec) {
        return repository.delete(accessController == null ? spec : accessController.appendAccessRules(Operation.DELETE, spec));
    }

    @Override
    public <S extends T, R> R findBy(@NonNull final Specification<T> spec,
            final Function<? super SpecificationFluentQuery<S>, R> queryFunction) {
        Objects.requireNonNull(spec, SPEC_MUST_NOT_BE_NULL);
        return repository.findBy(appendRules(Operation.READ, spec), queryFunction);
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
        return repository.findAllWithoutCount(appendRules(Operation.READ, spec), pageable);
    }

    @NonNull
    public Optional<T> findOne(final Operation operation, @NonNull final Specification<T> spec) {
        Objects.requireNonNull(spec, SPEC_MUST_NOT_BE_NULL);
        return repository.findOne(appendRules(operation, spec));
    }

    @Override
    @NonNull
    public List<T> findAll(final Operation operation, @Nullable final Specification<T> spec) {
        return repository.findAll(appendRules(operation, spec));
    }

    @Override
    public boolean exists(final Operation operation, final Specification<T> spec) {
        Objects.requireNonNull(spec, SPEC_MUST_NOT_BE_NULL);
        return repository.exists(appendRules(operation, spec));
    }

    @Override
    public long count(final Operation operation, @Nullable final Specification<T> spec) {
        return repository.count(appendRules(operation, spec));
    }

    @Override
    @NonNull
    public Slice<T> findAllWithoutCount(final Operation operation, @Nullable final Specification<T> spec, final Pageable pageable) {
        return repository.findAllWithoutCount(appendRules(operation, spec), pageable);
    }

    @Override
    @NonNull
    public Class<T> getDomainClass() {
        return repository.getDomainClass();
    }

    @Override
    public Optional<T> findOne(final Specification<T> spec, final String entityGraph) {
        return repository.findOne(appendRules(Operation.READ, spec), entityGraph);
    }

    @Override
    public List<T> findAll(final Specification<T> spec, final String entityGraph) {
        return repository.findAll(appendRules(Operation.READ, spec), entityGraph);
    }

    @Override
    public Page<T> findAll(final Specification<T> spec, final String entityGraph, final Pageable pageable) {
        return repository.findAll(appendRules(Operation.READ, spec), entityGraph, pageable);
    }

    @Override
    public List<T> findAll(final Specification<T> spec, final String entityGraph, final Sort sort) {
        return repository.findAll(appendRules(Operation.READ, spec), entityGraph, sort);
    }

    private Specification<T> appendRules(final Operation operation, @Nullable final Specification<T> spec) {
        if (accessController == null || operation == null) {
            return spec == null ? Specification.unrestricted() : spec;
        }
        return accessController.appendAccessRules(operation, spec);
    }

    private void assertAllowed(final Operation operation, final T entity) {
        if (accessController != null && operation != null) {
            accessController.assertOperationAllowed(operation, entity);
        }
    }

    private void assertAllowed(final Operation operation, final Iterable<? extends T> entities) {
        if (accessController != null && operation != null) {
            accessController.assertOperationAllowed(operation, entities);
        }
    }

    private boolean readable(final T entity) {
        if (accessController == null) {
            return true;
        }
        try {
            accessController.assertOperationAllowed(Operation.READ, entity);
            return true;
        } catch (final InsufficientPermissionException e) {
            return false;
        }
    }

    /**
     * Invokes {@code method} on {@code target}, unwrapping reflection's {@link InvocationTargetException} so the proxy
     * propagates the real (possibly checked) exception transparently instead of the reflection wrapper.
     */
    private static Object invokeUnwrapping(final Method method, final Object target, final Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (final InvocationTargetException e) {
            throw e.getCause() == null ? e : e.getCause();
        }
    }

    @SuppressWarnings({ "unchecked", "java:S3776" }) // java:S3776 - better readable in one places
    static <T extends AbstractJpaBaseEntity, R extends BaseEntityRepository<T>> R of(
            final R repository, @Nullable final AccessController<T> accessController) {
        Objects.requireNonNull(repository);
        final BaseEntityRepositoryProxy<T> repositoryACM = new BaseEntityRepositoryProxy<>(repository, accessController);
        // Resolve each proxied method once to the wrapper method that overrides it (or empty when the wrapper does not
        // declare it and the call falls through to the raw repository). Avoids a getDeclaredMethod lookup and an
        // exception-driven branch on every invocation.
        final Map<Method, Optional<Method>> delegateCache = new ConcurrentHashMap<>();
        final R acmProxy = (R) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                repository.getClass().getInterfaces(),
                (proxyInstance, method, args) -> {
                    final Optional<Method> delegate = delegateCache.computeIfAbsent(method, m -> {
                        try {
                            return Optional.of(BaseEntityRepositoryProxy.class.getDeclaredMethod(m.getName(), m.getParameterTypes()));
                        } catch (final NoSuchMethodException nsme) {
                            log.warn("RepositoryProxy does not override method {} - call will fall through to the raw repository", m, nsme);
                            return Optional.empty();
                        }
                    });
                    if (delegate.isPresent()) {
                        return invokeUnwrapping(delegate.get(), repositoryACM, args);
                    }
                    // call a repository method the wrapper does not override
                    if (method.getName().startsWith("find") || method.getName().startsWith("get")) {
                        final Object result = invokeUnwrapping(method, repository, args);
                        if (accessController != null) {
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
                        }
                        return result;
                    } else if ("toString".equals(method.getName()) && method.getParameterCount() == 0) {
                        return BaseEntityRepositoryProxy.class
                                .getSimpleName() + "(repository: " + repository + ", accessController: " + accessController + ")";
                    }
                    return invokeUnwrapping(method, repository, args);
                });
        log.debug("Repository wrapper created -> {}", acmProxy);
        return acmProxy;
    }
}
