/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.validation.ConstraintViolationException;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for {@link DistributionSetType}s.
 *
 */
public interface DistributionSetTypeManagement {

    /**
     * Creates new {@link DistributionSetType}.
     *
     * @param create
     *            to create
     * @return created entity
     * 
     * @throws EntityNotFoundException
     *             if a provided linked entity does not exists (
     *             {@link DistributionSetType#getMandatoryModuleTypes()} or
     *             {@link DistributionSetType#getOptionalModuleTypes()}
     * @throws ConstraintViolationException
     *             if fields are not filled as specified. Check
     *             {@link DistributionSetTypeCreate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    DistributionSetType createDistributionSetType(@NotNull DistributionSetTypeCreate create);

    /**
     * Creates multiple {@link DistributionSetType}s.
     *
     * @param creates
     *            to create
     * @return created entity
     * 
     * @throws EntityNotFoundException
     *             if a provided linked entity does not exists (
     *             {@link DistributionSetType#getMandatoryModuleTypes()} or
     *             {@link DistributionSetType#getOptionalModuleTypes()}
     * @throws ConstraintViolationException
     *             if fields are not filled as specified. Check
     *             {@link DistributionSetTypeCreate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    List<DistributionSetType> createDistributionSetTypes(@NotNull Collection<DistributionSetTypeCreate> creates);

    /**
     * Count all {@link DistributionSet}s in the repository that are not marked
     * as deleted.
     * 
     * @param typeId
     *            to look for
     *
     * @return number of {@link DistributionSet}s
     * 
     * @throws EntityNotFoundException
     *             if type with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Long countDistributionSetsByType(@NotNull Long typeId);

    /**
     * Deletes or mark as delete in case the type is in use.
     *
     * @param typeId
     *            to delete
     * 
     * @throws EntityNotFoundException
     *             if given set does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    void deleteDistributionSetType(@NotNull Long typeId);

    /**
     * @param key
     *            as {@link DistributionSetType#getKey()}
     * @return {@link DistributionSetType}
     */

    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<DistributionSetType> findDistributionSetTypeByKey(@NotEmpty String key);

    /**
     * @param name
     *            as {@link DistributionSetType#getName()}
     * @return {@link DistributionSetType}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<DistributionSetType> findDistributionSetTypeByName(@NotEmpty String name);

    /**
     * @param pageable
     *            parameter
     * @return all {@link DistributionSetType}s in the repository.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSetType> findDistributionSetTypesAll(@NotNull Pageable pageable);

    /**
     * Generic predicate based query for {@link DistributionSetType}.
     *
     * @param rsqlParam
     *            rsql query string
     * @param pageable
     *            parameter for paging
     *
     * @return the found {@link SoftwareModuleType}s
     * 
     * @throws RSQLParameterUnsupportedFieldException
     *             if a field in the RSQL string is used but not provided by the
     *             given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException
     *             if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSetType> findDistributionSetTypesAll(@NotNull String rsqlParam, @NotNull Pageable pageable);

    /**
     * @param id
     *            as {@link DistributionSetType#getId()}
     * @return {@link DistributionSetType}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<DistributionSetType> findDistributionSetTypeById(@NotNull Long id);

    /**
     * Updates existing {@link DistributionSetType}. Resets assigned
     * {@link SoftwareModuleType}s as well and sets as provided.
     *
     * @param update
     *            to update
     * 
     * @return updated entity
     * 
     * @throws EntityNotFoundException
     *             in case the {@link DistributionSetType} does not exists and
     *             cannot be updated
     * 
     * @throws EntityReadOnlyException
     *             if the {@link DistributionSetType} is already in use by a
     *             {@link DistributionSet} and user tries to change list of
     *             {@link SoftwareModuleType}s
     * @throws ConstraintViolationException
     *             if fields are not filled as specified. Check
     *             {@link DistributionSetTypeUpdate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSetType updateDistributionSetType(@NotNull DistributionSetTypeUpdate update);

    /**
     * Assigns {@link DistributionSetType#getMandatoryModuleTypes()}.
     * 
     * @param dsTypeId
     *            to update
     * @param softwareModuleTypeIds
     *            to assign
     * @return updated {@link DistributionSetType}
     * 
     * @throws EntityNotFoundException
     *             in case the {@link DistributionSetType} or at least one of
     *             the {@link SoftwareModuleType}s do not exist
     * 
     * @throws EntityReadOnlyException
     *             if the {@link DistributionSetType} while it is already in use
     *             by a {@link DistributionSet}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSetType assignOptionalSoftwareModuleTypes(@NotNull Long dsTypeId,
            @NotEmpty Collection<Long> softwareModuleTypeIds);

    /**
     * Assigns {@link DistributionSetType#getOptionalModuleTypes()}.
     * 
     * @param dsTypeId
     *            to update
     * @param softwareModuleTypes
     *            to assign
     * @return updated {@link DistributionSetType}
     * 
     * @throws EntityNotFoundException
     *             in case the {@link DistributionSetType} or at least one of
     *             the {@link SoftwareModuleType}s do not exist
     * 
     * @throws EntityReadOnlyException
     *             if the {@link DistributionSetType} while it is already in use
     *             by a {@link DistributionSet}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSetType assignMandatorySoftwareModuleTypes(@NotNull Long dsTypeId,
            @NotEmpty Collection<Long> softwareModuleTypes);

    /**
     * Unassigns a {@link SoftwareModuleType} from the
     * {@link DistributionSetType}. Does nothing if {@link SoftwareModuleType}
     * has not been assigned in the first place.
     * 
     * @param dsTypeId
     *            to update
     * @param softwareModuleId
     *            to unassign
     * @return updated {@link DistributionSetType}
     * 
     * @throws EntityNotFoundException
     *             in case the {@link DistributionSetType} does not exist
     * 
     * @throws EntityReadOnlyException
     *             if the {@link DistributionSetType} while it is already in use
     *             by a {@link DistributionSet}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSetType unassignSoftwareModuleType(@NotNull Long dsTypeId, @NotNull Long softwareModuleId);

    /**
     * @return number of {@link DistributionSetType}s in the repository.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Long countDistributionSetTypesAll();

}
