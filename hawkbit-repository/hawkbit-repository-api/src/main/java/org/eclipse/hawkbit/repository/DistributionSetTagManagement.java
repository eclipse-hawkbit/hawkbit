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

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.Tag;

/**
 * Management service for {@link DistributionSetTag}s.
 */
public interface DistributionSetTagManagement<T extends DistributionSetTag>
        extends RepositoryManagement<T, DistributionSetTagManagement.Create, DistributionSetTagManagement.Update> {

    @Override
    default String permissionGroup() {
        return SpPermission.DISTRIBUTION_SET;
    }

    @SuperBuilder
    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    final class Create extends UpdateCreate {}

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
        @Size(min = 1, max = NamedEntity.NAME_MAX_SIZE)
        @NotNull(groups = Create.class)
        private String name;

        @ValidString
        @Size(max = NamedEntity.DESCRIPTION_MAX_SIZE)
        private String description;

        @ValidString
        @Size(max = Tag.COLOUR_MAX_SIZE)
        private String colour;
    }
}