/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

/**
 * Provider that returns a base repository implementation dynamically based on repository type
 */
@FunctionalInterface
public interface BaseRepositoryTypeProvider {

    /**
     * Return a base repository implementation that shall be used based on provided repository type
     *
     * @param repositoryType type of repository
     * @return base repository implementation class
     */
    Class<?> getBaseRepositoryType(final Class<?> repositoryType);
}
