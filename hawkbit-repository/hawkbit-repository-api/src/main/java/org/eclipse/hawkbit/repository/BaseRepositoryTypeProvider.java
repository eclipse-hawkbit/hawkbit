/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

/**
 * Provider that returns a base repository implementation dynamically based on repository type
 */
@FunctionalInterface
public interface BaseRepositoryTypeProvider {

    /**
     * Return a base repository implementation that shall be used based on provided repository type
     * @param repositoryType type of repository
     * @return base repository implementation class
     */
    Class<?> getBaseRepositoryType(final Class<?> repositoryType);
}
