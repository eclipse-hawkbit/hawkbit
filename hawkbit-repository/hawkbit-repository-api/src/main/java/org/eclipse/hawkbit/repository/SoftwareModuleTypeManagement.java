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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.auth.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Type;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service for managing {@link SoftwareModuleType}s.
 */
public interface SoftwareModuleTypeManagement<T extends SoftwareModuleType>
        extends RepositoryManagement<T, SoftwareModuleTypeManagement.Create, SoftwareModuleTypeManagement.Update> {

    @Override
    default String permissionGroup() {
        return SpPermission.SOFTWARE_MODULE_TYPE;
    }

    /**
     * @param key to search for
     * @return {@link SoftwareModuleType} in the repository with given {@link SoftwareModuleType#getKey()}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Optional<T> findByKey(@NotEmpty String key);

    @SuperBuilder
    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    final class Create extends UpdateCreate {

        @ValidString
        @Size(min = 1, max = Type.KEY_MAX_SIZE)
        @NotNull
        private String key;

        @ValidString
        @Size(min = 1, max = NamedEntity.NAME_MAX_SIZE)
        @NotNull
        private String name;

        @Builder.Default
        private int maxAssignments = 1;
        @Builder.Default
        private int minArtifacts;
    }

    @SuperBuilder
    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    final class Update extends UpdateCreate implements Identifiable<Long> {

        @NotNull
        private Long id;
    }

    @SuperBuilder
    @Getter
    class UpdateCreate {

        @ValidString
        @Size(max = NamedEntity.DESCRIPTION_MAX_SIZE)
        private String description;

        @ValidString
        @Size(max = Type.COLOUR_MAX_SIZE)
        private String colour;
    }
}