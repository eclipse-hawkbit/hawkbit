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

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.auth.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Type;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for {@link DistributionSetType}s.
 */
public interface DistributionSetTypeManagement<T extends DistributionSetType>
        extends RepositoryManagement<T, DistributionSetTypeManagement.Create, DistributionSetTypeManagement.Update> {

    @Override
    default String permissionGroup() {
        return SpPermission.DISTRIBUTION_SET_TYPE;
    }

    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Optional<T> findByKey(@NotEmpty String key);

    /**
     * Assigns {@link DistributionSetType#getMandatoryModuleTypes()}.
     *
     * @param id to update
     * @param softwareModuleTypeIds to assign
     * @return updated {@link DistributionSetType}
     * @throws EntityNotFoundException in case the {@link DistributionSetType} or at least one of the {@link SoftwareModuleType}s do not exist
     * @throws EntityReadOnlyException if the {@link DistributionSetType} while it is already in use by a {@link DistributionSet}
     * @throws AssignmentQuotaExceededException if the maximum number of {@link SoftwareModuleType}s is exceeded for the addressed
     *         {@link DistributionSetType}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    T assignOptionalSoftwareModuleTypes(long id, @NotEmpty Collection<Long> softwareModuleTypeIds);

    /**
     * Assigns {@link DistributionSetType#getOptionalModuleTypes()}.
     *
     * @param id to update
     * @param softwareModuleTypeIds to assign
     * @return updated {@link DistributionSetType}
     * @throws EntityNotFoundException in case the {@link DistributionSetType} or at least one of the {@link SoftwareModuleType}s do not exist
     * @throws EntityReadOnlyException if the {@link DistributionSetType} while it is already in use by a {@link DistributionSet}
     * @throws AssignmentQuotaExceededException if the maximum number of {@link SoftwareModuleType}s is exceeded for the addressed
     *         {@link DistributionSetType}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    T assignMandatorySoftwareModuleTypes(long id, @NotEmpty Collection<Long> softwareModuleTypeIds);

    /**
     * Unassigns a {@link SoftwareModuleType} from the {@link DistributionSetType}. Does nothing if {@link SoftwareModuleType}
     * has not been assigned in the first place.
     *
     * @param id to update
     * @param softwareModuleTypeId to unassign
     * @return updated {@link DistributionSetType}
     * @throws EntityNotFoundException in case the {@link DistributionSetType} does not exist
     * @throws EntityReadOnlyException if the {@link DistributionSetType} while it is already in use by a {@link DistributionSet}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    T unassignSoftwareModuleType(long id, long softwareModuleTypeId);

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

        private Set<? extends SoftwareModuleType> mandatoryModuleTypes;
        private Set<? extends SoftwareModuleType> optionalModuleTypes;
    }
}