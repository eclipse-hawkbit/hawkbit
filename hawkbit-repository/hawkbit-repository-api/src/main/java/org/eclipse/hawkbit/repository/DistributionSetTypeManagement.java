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

import jakarta.validation.constraints.NotEmpty;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeUpdate;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for {@link DistributionSetType}s.
 */
public interface DistributionSetTypeManagement
        extends RepositoryManagement<DistributionSetType, DistributionSetTypeCreate, DistributionSetTypeUpdate> {

    /**
     * @param key as {@link DistributionSetType#getKey()}
     * @return {@link DistributionSetType}
     */

    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<DistributionSetType> getByKey(@NotEmpty String key);

    /**
     * @param name as {@link DistributionSetType#getName()}
     * @return {@link DistributionSetType}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<DistributionSetType> getByName(@NotEmpty String name);

    /**
     * Assigns {@link DistributionSetType#getMandatoryModuleTypes()}.
     *
     * @param id to update
     * @param softwareModuleTypeIds to assign
     * @return updated {@link DistributionSetType}
     * @throws EntityNotFoundException in case the {@link DistributionSetType} or at least one of
     *         the {@link SoftwareModuleType}s do not exist
     * @throws EntityReadOnlyException if the {@link DistributionSetType} while it is already in use
     *         by a {@link DistributionSet}
     * @throws AssignmentQuotaExceededException if the maximum number of {@link SoftwareModuleType}s is
     *         exceeded for the addressed {@link DistributionSetType}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSetType assignOptionalSoftwareModuleTypes(long id, @NotEmpty Collection<Long> softwareModuleTypeIds);

    /**
     * Assigns {@link DistributionSetType#getOptionalModuleTypes()}.
     *
     * @param id to update
     * @param softwareModuleTypeIds to assign
     * @return updated {@link DistributionSetType}
     * @throws EntityNotFoundException in case the {@link DistributionSetType} or at least one of
     *         the {@link SoftwareModuleType}s do not exist
     * @throws EntityReadOnlyException if the {@link DistributionSetType} while it is already in use
     *         by a {@link DistributionSet}
     * @throws AssignmentQuotaExceededException if the maximum number of {@link SoftwareModuleType}s is
     *         exceeded for the addressed {@link DistributionSetType}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSetType assignMandatorySoftwareModuleTypes(long id, @NotEmpty Collection<Long> softwareModuleTypeIds);

    /**
     * Unassigns a {@link SoftwareModuleType} from the
     * {@link DistributionSetType}. Does nothing if {@link SoftwareModuleType}
     * has not been assigned in the first place.
     *
     * @param id to update
     * @param softwareModuleTypeId to unassign
     * @return updated {@link DistributionSetType}
     * @throws EntityNotFoundException in case the {@link DistributionSetType} does not exist
     * @throws EntityReadOnlyException if the {@link DistributionSetType} while it is already in use
     *         by a {@link DistributionSet}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSetType unassignSoftwareModuleType(long id, long softwareModuleTypeId);
}
