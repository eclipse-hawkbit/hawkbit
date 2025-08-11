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

import static org.eclipse.hawkbit.im.authentication.SpringEvalExpressions.BRACKET_CLOSE;
import static org.eclipse.hawkbit.im.authentication.SpringEvalExpressions.BRACKET_OPEN;
import static org.eclipse.hawkbit.im.authentication.SpringEvalExpressions.HAS_AUTH_AND;
import static org.eclipse.hawkbit.im.authentication.SpringEvalExpressions.HAS_AUTH_PREFIX;
import static org.eclipse.hawkbit.im.authentication.SpringEvalExpressions.HAS_AUTH_READ_TARGET_TYPE;
import static org.eclipse.hawkbit.im.authentication.SpringEvalExpressions.HAS_AUTH_SUFFIX;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.exception.TargetTypeKeyOrNameRequiredException;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.Type;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for {@link TargetType}s.
 */
public interface TargetTypeManagement<T extends TargetType>
        extends RepositoryManagement<T, TargetTypeManagement.Create, TargetTypeManagement.Update> {

    String HAS_AUTH_READ_DISTRIBUTION_SET_AND_UPDATE_TARGET_TYPE = BRACKET_OPEN +
            HAS_AUTH_PREFIX + SpPermission.READ_DISTRIBUTION_SET + HAS_AUTH_SUFFIX +
            HAS_AUTH_AND +
            HAS_AUTH_PREFIX + SpPermission.UPDATE_TARGET_TYPE + HAS_AUTH_SUFFIX +
            BRACKET_CLOSE;

    @Override
    default String permissionGroup() {
        return "TARGET_TYPE";
    }

    /**
     * @param key as {@link TargetType#getKey()}
     * @return {@link TargetType}
     */
    @PreAuthorize(HAS_AUTH_READ_TARGET_TYPE)
    Optional<TargetType> getByKey(@NotEmpty String key);

    /**
     * @param name as {@link TargetType#getName()}
     * @return {@link TargetType}
     */
    @PreAuthorize(HAS_AUTH_READ_TARGET_TYPE)
    Optional<TargetType> getByName(@NotEmpty String name);

    /**
     * @param id Target type ID
     * @param distributionSetTypeIds Distribution set ID
     * @return Target type
     */
    @PreAuthorize(HAS_AUTH_READ_DISTRIBUTION_SET_AND_UPDATE_TARGET_TYPE)
    TargetType assignCompatibleDistributionSetTypes(long id, @NotEmpty Collection<Long> distributionSetTypeIds);

    /**
     * @param id Target type ID
     * @param distributionSetTypeIds Distribution set ID
     * @return Target type
     */
    @PreAuthorize(HAS_AUTH_READ_DISTRIBUTION_SET_AND_UPDATE_TARGET_TYPE)
    TargetType unassignDistributionSetType(long id, long distributionSetTypeIds);

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

        private Set<DistributionSetType> distributionSetTypes;

        @SuppressWarnings("java:S1144") // java:S1144 - constructor is actually used by SuperBuilder's build() method
        private Create(final CreateBuilder<?, ?> builder) {
            super(builder);
            if (builder.key == null && builder.name == null) {
                throw new TargetTypeKeyOrNameRequiredException("Key or name of the target type shall be specified!");
            }
            key = builder.key == null ? builder.name : builder.key;
            name = builder.name == null ? builder.key : builder.name;
            distributionSetTypes = builder.distributionSetTypes == null ? Collections.emptySet() : builder.distributionSetTypes;
        }
    }

    @SuperBuilder
    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    final class Update extends UpdateCreate implements Identifiable<Long> {

        @NotNull
        private Long id;

        @ValidString
        @Size(min = 1, max = NamedEntity.NAME_MAX_SIZE)
        private String name;
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