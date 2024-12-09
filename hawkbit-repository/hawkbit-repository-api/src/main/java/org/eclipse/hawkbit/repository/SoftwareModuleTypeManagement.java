/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import java.util.Optional;

import jakarta.validation.constraints.NotEmpty;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeUpdate;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service for managing {@link SoftwareModuleType}s.
 */
public interface SoftwareModuleTypeManagement
        extends RepositoryManagement<SoftwareModuleType, SoftwareModuleTypeCreate, SoftwareModuleTypeUpdate> {

    /**
     * @param key to search for
     * @return {@link SoftwareModuleType} in the repository with given {@link SoftwareModuleType#getKey()}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<SoftwareModuleType> getByKey(@NotEmpty String key);

    /**
     * @param name to search for
     * @return all {@link SoftwareModuleType}s in the repository with given {@link SoftwareModuleType#getName()}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<SoftwareModuleType> getByName(@NotEmpty String name);
}