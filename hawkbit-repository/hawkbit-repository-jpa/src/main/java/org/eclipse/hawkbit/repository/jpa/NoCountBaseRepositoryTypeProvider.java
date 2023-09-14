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
