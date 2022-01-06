/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.BaseRepositoryTypeProvider;

/**
 * Simple implementation of {@link BaseRepositoryTypeProvider} leveraging our
 * {@link SimpleJpaWithNoCountRepository} for all current use cases
 */
public class NoCountBaseRepositoryTypeProvider implements BaseRepositoryTypeProvider {

    @Override
    public Class<?> getBaseRepositoryType(final Class<?> repositoryType) {
        return SimpleJpaWithNoCountRepository.class;
    }

}
