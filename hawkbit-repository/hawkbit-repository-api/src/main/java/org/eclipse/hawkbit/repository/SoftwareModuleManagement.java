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
import org.eclipse.hawkbit.repository.builder.SoftwareModuleCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleUpdate;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.model.AssignedSoftwareModule;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service for managing {@link SoftwareModule}s.
 *
 */
public interface SoftwareModuleManagement {

    /**
     * Counts {@link SoftwareModule}s with given
     * {@link SoftwareModule#getName()} or {@link SoftwareModule#getVersion()}
     * and {@link SoftwareModule#getType()} that are not marked as deleted.
     *
     * @param searchText
     *            to search for in name and version
     * @param typeId
     *            to filter the result by type
     * @return number of found {@link SoftwareModule}s
     * 
     * @throws EntityNotFoundException
     *             if software module type with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Long countSoftwareModuleByFilters(String searchText, Long typeId);

    /**
     * Count all {@link SoftwareModule}s in the repository that are not marked
     * as deleted.
     *
     * @return number of {@link SoftwareModule}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Long countSoftwareModulesAll();

    /**
     * Create {@link SoftwareModule}s in the repository.
     *
     * @param creates
     *            {@link SoftwareModule}s to create
     * @return SoftwareModule
     * 
     * @throws EntityAlreadyExistsException
     *             if a given entity already exists
     * @throws EntityNotFoundException
     *             of given software module type does not exist
     * @throws ConstraintViolationException
     *             if fields are not filled as specified. Check
     *             {@link SoftwareModuleCreate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    List<SoftwareModule> createSoftwareModule(@NotNull Collection<SoftwareModuleCreate> creates);

    /**
     *
     * @param create
     *            SoftwareModule to create
     * @return SoftwareModule
     * 
     * @throws EntityAlreadyExistsException
     *             if a given entity already exists
     * @throws EntityNotFoundException
     *             of given software module type does not exist
     * @throws ConstraintViolationException
     *             if fields are not filled as specified. Check
     *             {@link SoftwareModuleCreate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    SoftwareModule createSoftwareModule(@NotNull SoftwareModuleCreate create);

    /**
     * creates a list of software module meta data entries.
     * 
     * @param moduleId
     *            the metadata belongs to
     * @param metadata
     *            the meta data entries to create or update
     * @return the updated or created software module meta data entries
     * @throws EntityAlreadyExistsException
     *             in case one of the meta data entry already exists for the
     *             specific key
     * @throws EntityNotFoundException
     *             if software module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    List<SoftwareModuleMetadata> createSoftwareModuleMetadata(@NotNull Long moduleId,
            @NotNull Collection<MetaData> metadata);

    /**
     * creates or updates a single software module meta data entry.
     * 
     * @param moduleId
     *            the metadata belongs to
     * @param metadata
     *            the meta data entry to create or update
     * @return the updated or created software module meta data entry
     * @throws EntityAlreadyExistsException
     *             in case the meta data entry already exists for the specific
     *             key
     * @throws EntityNotFoundException
     *             if software module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    SoftwareModuleMetadata createSoftwareModuleMetadata(@NotNull Long moduleId, @NotNull MetaData metadata);

    /**
     * Deletes the given {@link SoftwareModule} Entity.
     *
     * @param moduleId
     *            is the {@link SoftwareModule} to be deleted
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    void deleteSoftwareModule(@NotNull Long moduleId);

    /**
     * deletes a software module meta data entry.
     *
     * @param moduleId
     *            where meta data has to be deleted
     * @param key
     *            of the metda data element
     * 
     * @throws EntityNotFoundException
     *             of module or metadata entry does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    void deleteSoftwareModuleMetadata(@NotNull Long moduleId, @NotEmpty String key);

    /**
     * Deletes {@link SoftwareModule}s which is any if the given ids.
     *
     * @param moduleIds
     *            of the Software Modules to be deleted
     * 
     * @throws EntityNotFoundException
     *             if (at least one) module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    void deleteSoftwareModules(@NotNull Collection<Long> moduleIds);

    /**
     * @param pageable
     *            the page request to page the result set
     * @param setId
     *            to search for
     * @return all {@link SoftwareModule}s that are assigned to given
     *         {@link DistributionSet}.
     * 
     * @throws EntityNotFoundException
     *             if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<SoftwareModule> findSoftwareModuleByAssignedTo(@NotNull Pageable pageable, @NotNull Long setId);

    /**
     * Filter {@link SoftwareModule}s with given
     * {@link SoftwareModule#getName()} or {@link SoftwareModule#getVersion()}
     * and {@link SoftwareModule#getType()} that are not marked as deleted.
     *
     * @param pageable
     *            page parameter
     * @param searchText
     *            to be filtered as "like" on {@link SoftwareModule#getName()}
     * @param typeId
     *            to be filtered as "like" on {@link SoftwareModule#getType()}
     * @return the page of found {@link SoftwareModule}
     * 
     * @throws EntityNotFoundException
     *             if given software module type does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Slice<SoftwareModule> findSoftwareModuleByFilters(@NotNull Pageable pageable, String searchText, Long typeId);

    /**
     * Finds {@link SoftwareModuleType} by given id.
     *
     * @param ids
     *            to search for
     * @return the found {@link SoftwareModuleType}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    List<SoftwareModuleType> findSoftwareModuleTypesById(@NotEmpty Collection<Long> ids);

    /**
     * Finds {@link SoftwareModule} by given id.
     *
     * @param id
     *            to search for
     * @return the found {@link SoftwareModule}s
     * 
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    Optional<SoftwareModule> findSoftwareModuleById(@NotNull Long id);

    /**
     * retrieves {@link SoftwareModule} by their name AND version AND type..
     *
     * @param name
     *            of the {@link SoftwareModule}
     * @param version
     *            of the {@link SoftwareModule}
     * @param typeId
     *            of the {@link SoftwareModule}
     * @return the found {@link SoftwareModule}
     * 
     * @throws EntityNotFoundException
     *             if software module type with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<SoftwareModule> findSoftwareModuleByNameAndVersion(@NotEmpty String name, @NotEmpty String version,
            @NotNull Long typeId);

    /**
     * finds a single software module meta data by its id.
     *
     * @param moduleId
     *            where meta data has to be found
     * @param key
     *            of the meta data element
     * @return the found SoftwareModuleMetadata or {@code null} if not exits
     * 
     * @throws EntityNotFoundException
     *             is module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<SoftwareModuleMetadata> findSoftwareModuleMetadata(@NotNull Long moduleId, @NotEmpty String key);

    /**
     * finds all meta data by the given software module id.
     *
     * @param swId
     *            the software module id to retrieve the meta data from
     * @param pageable
     *            the page request to page the result
     * @return a paged result of all meta data entries for a given software
     *         module id
     * 
     * @throws EntityNotFoundException
     *             if software module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<SoftwareModuleMetadata> findSoftwareModuleMetadataBySoftwareModuleId(@NotNull Long swId,
            @NotNull Pageable pageable);

    /**
     * finds all meta data by the given software module id.
     *
     * @param moduleId
     *            the software module id to retrieve the meta data from
     * @param rsqlParam
     *            filter definition in RSQL syntax
     * @param pageable
     *            the page request to page the result
     * @return a paged result of all meta data entries for a given software
     *         module id
     * 
     * @throws RSQLParameterUnsupportedFieldException
     *             if a field in the RSQL string is used but not provided by the
     *             given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException
     *             if the RSQL syntax is wrong
     * @throws EntityNotFoundException
     *             if software module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<SoftwareModuleMetadata> findSoftwareModuleMetadataBySoftwareModuleId(@NotNull Long moduleId,
            @NotNull String rsqlParam, @NotNull Pageable pageable);

    /**
     * Finds all meta data by the given software module id.
     * 
     * @param pageable
     *            pagination parameter
     *
     * @param moduleId
     *            the software module id to retrieve the meta data from
     * @return page with found software module metadata
     * 
     * @throws EntityNotFoundException
     *             of software module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<SoftwareModuleMetadata> findSoftwareModuleMetadataBySoftwareModuleId(@NotNull Pageable pageable,
            @NotNull Long moduleId);

    /**
     * Filter {@link SoftwareModule}s with given
     * {@link SoftwareModule#getName()} or {@link SoftwareModule#getVersion()}
     * search text and {@link SoftwareModule#getType()} that are not marked as
     * deleted and sort them by means of given distribution set related modules
     * on top of the list.
     * 
     * After that the modules are sorted by {@link SoftwareModule#getName()} and
     * {@link SoftwareModule#getVersion()} in ascending order.
     *
     * @param pageable
     *            page parameter
     * @param orderByDistributionId
     *            the ID of distribution set to be ordered on top
     * @param searchText
     *            filtered as "like" on {@link SoftwareModule#getName()}
     * @param typeId
     *            filtered as "equal" on {@link SoftwareModule#getType()}
     * @return the page of found {@link SoftwareModule}
     * 
     * @throws EntityNotFoundException
     *             if given software module type does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Slice<AssignedSoftwareModule> findSoftwareModuleOrderBySetAssignmentAndModuleNameAscModuleVersionAsc(
            @NotNull Pageable pageable, @NotNull Long orderByDistributionId, String searchText, Long typeId);

    /**
     * Retrieves all software modules. Deleted ones are filtered.
     *
     * @param pageable
     *            pagination parameter
     * @return the found {@link SoftwareModule}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Slice<SoftwareModule> findSoftwareModulesAll(@NotNull Pageable pageable);

    /**
     * Retrieves all software modules with a given list of ids
     * {@link SoftwareModule#getId()}.
     *
     * @param ids
     *            to search for
     * @return {@link List} of found {@link SoftwareModule}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    List<SoftwareModule> findSoftwareModulesById(@NotEmpty Collection<Long> ids);

    /**
     * Retrieves all {@link SoftwareModule}s with a given specification.
     *
     * @param rsqlParam
     *            filter definition in RSQL syntax
     * @param pageable
     *            pagination parameter
     * @return the found {@link SoftwareModule}s
     * 
     * @throws RSQLParameterUnsupportedFieldException
     *             if a field in the RSQL string is used but not provided by the
     *             given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException
     *             if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<SoftwareModule> findSoftwareModulesByPredicate(@NotNull String rsqlParam, @NotNull Pageable pageable);

    /**
     * retrieves the {@link SoftwareModule}s by their {@link SoftwareModuleType}
     * .
     *
     * @param pageable
     *            page parameters
     * @param typeId
     *            to be filtered on
     * @return the found {@link SoftwareModule}s
     * 
     * @throws EntityNotFoundException
     *             if software module type with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Slice<SoftwareModule> findSoftwareModulesByType(@NotNull Pageable pageable, @NotNull Long typeId);

    /**
     * Updates existing {@link SoftwareModule}. Update-able values are
     * {@link SoftwareModule#getDescription()}
     * {@link SoftwareModule#getVendor()}.
     *
     * @param update
     *            contains properties to update
     * 
     * @throws EntityNotFoundException
     *             if given module does not exist
     *
     * @return the saved Entity.
     * 
     * @throws EntityNotFoundException
     *             if given {@link SoftwareModule} does not exist
     * @throws ConstraintViolationException
     *             if fields are not filled as specified. Check
     *             {@link SoftwareModuleUpdate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    SoftwareModule updateSoftwareModule(@NotNull SoftwareModuleUpdate update);

    /**
     * updates a distribution set meta data value if corresponding entry exists.
     * 
     * @param moduleId
     *            the metadata belongs to
     * @param metadata
     *            the meta data entry to be updated
     * 
     * @return the updated meta data entry
     * 
     * @throws EntityNotFoundException
     *             in case the meta data entry does not exists and cannot be
     *             updated
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    SoftwareModuleMetadata updateSoftwareModuleMetadata(@NotNull Long moduleId, @NotNull MetaData metadata);
}
