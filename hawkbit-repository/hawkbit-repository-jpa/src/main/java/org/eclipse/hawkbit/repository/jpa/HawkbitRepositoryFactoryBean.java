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

import javax.persistence.EntityManager;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import reactor.util.annotation.NonNull;

/**
 * Extends the default {@link JpaRepositoryFactoryBean} by hawkbit specific
 * extensions:
 * 
 * BaseClass: {@link SimpleJpaWithNoCountRepository} adds findAllWithoutCount
 * implementation to proxy objects
 */
public class HawkbitRepositoryFactoryBean<R extends JpaRepository<T, I>, T, I extends Serializable>
        extends JpaRepositoryFactoryBean<R, T, I> {

    public HawkbitRepositoryFactoryBean(final Class<? extends R> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    @NonNull
    protected RepositoryFactorySupport createRepositoryFactory(EntityManager em) {
        RepositoryFactorySupport factory = super.createRepositoryFactory(em);
        factory.setRepositoryBaseClass(SimpleJpaWithNoCountRepository.class);
        return factory;
    }
}
