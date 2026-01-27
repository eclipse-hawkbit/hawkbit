/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.jpa.repository.HawkbitBaseRepository;
import org.eclipse.hawkbit.repository.jpa.utils.ExceptionMapper;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.jpa.repository.query.JpaQueryMethodFactory;
import org.springframework.data.jpa.repository.query.QueryEnhancerSelector;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.jpa.repository.support.JpaRepositoryFragmentsContributor;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;

/**
 * A {@link TransactionalRepositoryFactoryBeanSupport} extension that uses {@link HawkbitBaseRepository} as base repository and
 * proxied repositories in order to convert exceptions to management exceptions.
 */
@Slf4j
@SuppressWarnings("java:S119") // java:S119 - ID is inherited from TransactionalRepositoryFactoryBeanSupport
public class HawkbitBaseRepositoryFactoryBean<T extends Repository<S, ID>, S, ID> extends TransactionalRepositoryFactoryBeanSupport<T, S, ID> {

    private EntityPathResolver entityPathResolver;
    private JpaRepositoryFragmentsContributor repositoryFragmentsContributor;
    private EscapeCharacter escapeCharacter = EscapeCharacter.DEFAULT;
    private JpaQueryMethodFactory queryMethodFactory;

    @Nullable
    private EntityManager entityManager;

    /**
     * Creates a new {@link JpaRepositoryFactoryBean} for the given repository interface.
     *
     * @param repositoryInterface must not be {@literal null}.
     */
    public HawkbitBaseRepositoryFactoryBean(final Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    @Autowired
    public void setEntityPathResolver(final ObjectProvider<EntityPathResolver> resolver) {
        this.entityPathResolver = resolver.getIfAvailable(() -> SimpleEntityPathResolver.INSTANCE);
    }

    @Autowired
    public void setRepositoryFragmentsContributor(final ObjectProvider<JpaRepositoryFragmentsContributor> repositoryFragmentsContributor) {
        this.repositoryFragmentsContributor = repositoryFragmentsContributor.getIfAvailable(() -> JpaRepositoryFragmentsContributor.DEFAULT);
    }

    @Autowired
    public void setEscapeCharacter(final char escapeCharacter) {
        this.escapeCharacter = EscapeCharacter.of(escapeCharacter);
    }

    @Autowired
    public void setQueryMethodFactory(@Nullable final JpaQueryMethodFactory factory) {
        if (factory != null) {
            this.queryMethodFactory = factory;
        }
    }

    @Autowired
    public void setQueryMethodFactory(final ObjectProvider<JpaQueryMethodFactory> resolver) {
        final JpaQueryMethodFactory factory = resolver.getIfAvailable();
        if (factory != null) {
            this.queryMethodFactory = factory;
        }
    }

    private @Nullable BeanFactory beanFactory;

    @Override
    public void setBeanFactory(final BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        super.setBeanFactory(beanFactory);
    }

    private @Nullable Function<@Nullable BeanFactory, QueryEnhancerSelector> queryEnhancerSelectorSource;

    public void setQueryEnhancerSelectorSource(QueryEnhancerSelector queryEnhancerSelectorSource) {
        this.queryEnhancerSelectorSource = bf -> queryEnhancerSelectorSource;
    }

    public void setQueryEnhancerSelector(final Class<? extends QueryEnhancerSelector> queryEnhancerSelectorType) {
        queryEnhancerSelectorSource = bf -> {
            if (bf != null) {
                final QueryEnhancerSelector selector = bf.getBeanProvider(queryEnhancerSelectorType).getIfAvailable();
                if (selector != null) {
                    return selector;
                }

                if (bf instanceof AutowireCapableBeanFactory acbf) {
                    return acbf.createBean(queryEnhancerSelectorType);
                }
            }

            return BeanUtils.instantiateClass(queryEnhancerSelectorType);
        };
    }

    @PersistenceContext
    public void setEntityManager(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void afterPropertiesSet() {
        Objects.requireNonNull(entityManager, "EntityManager must not be null");
        setRepositoryBaseClass(HawkbitBaseRepository.class); // overrides set by properties base class
        super.afterPropertiesSet();
    }

    @Override
    public void setMappingContext(final MappingContext<?, ?> mappingContext) {
        super.setMappingContext(mappingContext);
    }

    @Override
    protected RepositoryFactorySupport doCreateRepositoryFactory() {
        Objects.requireNonNull(entityManager, "EntityManager must not be null");
        final JpaRepositoryFactory jpaRepositoryFactory = new JpaRepositoryFactory(entityManager) {

            @Override
            protected JpaRepositoryImplementation<?, ?> getTargetRepository(
                    final RepositoryInformation information, final EntityManager entityManager) {
                final JpaRepositoryImplementation<?, ?> jpaRepositoryImplementation = super.getTargetRepository(information, entityManager);
                return (JpaRepositoryImplementation<?, ?>) Proxy.newProxyInstance(
                        jpaRepositoryImplementation.getClass().getClassLoader(),
                        interfaces(jpaRepositoryImplementation.getClass(), new HashSet<>()).toArray(new Class<?>[0]),
                        (proxy, method, args) -> {
                            try {
                                if (args != null) {
                                    final Class<?>[] parameterTypes = method.getParameterTypes();
                                    for (int i = 0; i < parameterTypes.length; i++) {
                                        if (parameterTypes[i] == Specification.class && args[i] == null) {
                                            // replaces null specifications with unrestricted specifications (null not accepted since Spring Boott 4.0
                                            if (log.isTraceEnabled()) {
                                                log.trace("Replace null Specification argument with unrestricted Specification",
                                                        new Exception("Method " + method + ", arg[" + i + "]"));
                                            }
                                            args[i] = Specification.unrestricted();
                                        }
                                    }
                                }
                                return method.invoke(jpaRepositoryImplementation, args);
                            } catch (final InvocationTargetException e) {
                                final Throwable cause = e.getCause() == null ? e : e.getCause();
                                throw cause instanceof Exception exc ? ExceptionMapper.map(exc) : e;
                            } catch (final Exception e) {
                                throw ExceptionMapper.map(e);
                            }
                        });
            }
        };

        jpaRepositoryFactory.setEntityPathResolver(entityPathResolver);
        jpaRepositoryFactory.setEscapeCharacter(escapeCharacter);
        jpaRepositoryFactory.setFragmentsContributor(repositoryFragmentsContributor);
        if (queryMethodFactory != null) {
            jpaRepositoryFactory.setQueryMethodFactory(queryMethodFactory);
        }
        if (queryEnhancerSelectorSource != null) {
            jpaRepositoryFactory.setQueryEnhancerSelector(queryEnhancerSelectorSource.apply(beanFactory));
        }
        jpaRepositoryFactory.setRepositoryBaseClass(HawkbitBaseRepository.class);
        return jpaRepositoryFactory;
    }

    private static Set<Class<?>> interfaces(final Class<?> type, final Set<Class<?>> interfaces) {
        Collections.addAll(interfaces, type.getInterfaces());
        final Class<?> superclass = type.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            return interfaces(superclass, interfaces);
        } else {
            return interfaces;
        }
    }
}