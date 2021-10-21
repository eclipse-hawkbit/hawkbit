/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import javax.persistence.EntityManager;

import org.eclipse.hawkbit.repository.BaseRepositoryTypeProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * A {@link JpaRepositoryFactoryBean} extension that allow injection of custom
 * repository factories by using a {@link BaseRepositoryTypeProvider}
 * implementation, allows injecting different base repository implementations based on repository type
 * 
 * @param <T>
 * @param <S>
 * @param <ID>
 */
public class CustomBaseRepositoryFactoryBean<T extends Repository<S, ID>, S, ID>
        extends JpaRepositoryFactoryBean<T, S, ID> {

    @Autowired
    BaseRepositoryTypeProvider baseRepoProvider;

    /**
     * Creates a new {@link JpaRepositoryFactoryBean} for the given repository
     * interface.
     *
     * @param repositoryInterface
     *            must not be {@literal null}.
     */
    public CustomBaseRepositoryFactoryBean(final Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory(final EntityManager entityManager) {
        final RepositoryFactorySupport rfs = super.createRepositoryFactory(entityManager);
        rfs.setRepositoryBaseClass(baseRepoProvider.getBaseRepositoryType(getObjectType()));
        return rfs;
    }
}
