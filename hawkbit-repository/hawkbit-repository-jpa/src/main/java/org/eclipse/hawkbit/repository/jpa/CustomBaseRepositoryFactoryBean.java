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

import jakarta.persistence.EntityManager;

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
     * @param repositoryInterface must not be {@literal null}.
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
