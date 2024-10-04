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
import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataUpdate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleUpdate;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.model.AssignedSoftwareModule;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service for managing {@link SoftwareModule}s.
 *
 */
public interface SoftwareModuleManagement
        extends RepositoryManagement<SoftwareModule, SoftwareModuleCreate, SoftwareModuleUpdate> {

    /**
     * Creates a list of software module meta data entries.
     *
     * @param metadata
     *            the meta data entries to create
     *
     * @return the updated or created software module meta data entries
     *
     * @throws EntityAlreadyExistsException
     *             in case one of the meta data entry already exists for the
     *             specific key
     *
     * @throws EntityNotFoundException
     *             if software module with given ID does not exist
     *
     * @throws AssignmentQuotaExceededException
     *             if the maximum number of {@link SoftwareModuleMetadata}
     *             entries is exceeded for the addressed {@link SoftwareModule}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    List<SoftwareModuleMetadata> createMetaData(@NotNull @Valid Collection<SoftwareModuleMetadataCreate> metadata);

    /**
     * Creates or updates a single software module meta data entry.
     *
     * @param metadata
     *            the meta data entry to create
     *
     * @return the updated or created software module meta data entry
     *
     * @throws EntityAlreadyExistsException
     *             in case the meta data entry already exists for the specific
     *             key
     *
     * @throws EntityNotFoundException
     *             if software module with given ID does not exist
     *
     * @throws AssignmentQuotaExceededException
     *             if the maximum number of {@link SoftwareModuleMetadata}
     *             entries is exceeded for the addressed {@link SoftwareModule}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    SoftwareModuleMetadata createMetaData(@NotNull @Valid SoftwareModuleMetadataCreate metadata);

    /**
     * Deletes a software module meta data entry.
     *
     * @param id
     *            where meta data has to be deleted
     * @param key
     *            of the metda data element
     *
     * @throws EntityNotFoundException
     *             of module or metadata entry does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    void deleteMetaData(long id, @NotEmpty String key);

    /**
     * Returns all modules assigned to given {@link DistributionSet}.
     *
     * @param pageable
     *            the page request to page the result set
     * @param distributionSetId
     *            to search for
     *
     * @return all {@link SoftwareModule}s that are assigned to given
     *         {@link DistributionSet}.
     *
     * @throws EntityNotFoundException
     *             if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<SoftwareModule> findByAssignedTo(@NotNull Pageable pageable, long distributionSetId);

    /**
     * Returns count of all modules assigned to given {@link DistributionSet}.
     *
     * @param distributionSetId
     *            to search for
     *
     * @return count of {@link SoftwareModule}s that are assigned to given
     *         {@link DistributionSet}.
     *
     * @throws EntityNotFoundException
     *             if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    long countByAssignedTo(long distributionSetId);

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
     *
     * @return the page of found {@link SoftwareModule}
     *
     * @throws EntityNotFoundException
     *             if given software module type does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Slice<SoftwareModule> findByTextAndType(@NotNull Pageable pageable, String searchText, Long typeId);

    /**
     * Retrieves {@link SoftwareModule} by their name AND version AND type..
     *
     * @param name
     *            of the {@link SoftwareModule}
     * @param version
     *            of the {@link SoftwareModule}
     * @param typeId
     *            of the {@link SoftwareModule}
     *
     * @return the found {@link SoftwareModule}
     *
     * @throws EntityNotFoundException
     *             if software module type with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<SoftwareModule> getByNameAndVersionAndType(@NotEmpty String name, @NotEmpty String version, long typeId);

    /**
     * Finds a single software module meta data by its id.
     *
     * @param id
     *            where meta data has to be found
     * @param key
     *            of the meta data element
     * @return the found SoftwareModuleMetadata or {@code null} if not exits
     *
     * @throws EntityNotFoundException
     *             is module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<SoftwareModuleMetadata> getMetaDataBySoftwareModuleId(long id, @NotEmpty String key);

    /**
     * Finds all meta data by the given software module id.
     *
     * @param pageable
     *            the page request to page the result
     * @param id
     *            the software module id to retrieve the meta data from
     *
     * @return a paged result of all meta data entries for a given software
     *         module id
     *
     * @throws EntityNotFoundException
     *             if software module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<SoftwareModuleMetadata> findMetaDataBySoftwareModuleId(@NotNull Pageable pageable, long id);

    /**
     * Counts all meta data by the given software module id.
     *
     * @param id
     *            the software module id to retrieve the meta data count from
     *
     * @return count of all meta data entries for a given software module id
     *
     * @throws EntityNotFoundException
     *             if software module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    long countMetaDataBySoftwareModuleId(long id);

    /**
     * Finds all meta data by the given software module id where
     * {@link SoftwareModuleMetadata#isTargetVisible()}.
     *
     * @param pageable
     *            the page request to page the result
     * @param id
     *            the software module id to retrieve the meta data from
     *
     * @return a paged result of all meta data entries for a given software
     *         module id
     *
     * @throws EntityNotFoundException
     *             if software module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<SoftwareModuleMetadata> findMetaDataBySoftwareModuleIdAndTargetVisible(@NotNull Pageable pageable,
            long id);

    /**
     * Finds all meta data by the given software module id.
     *
     * @param pageable
     *            the page request to page the result
     * @param id
     *            the software module id to retrieve the meta data from
     * @param rsqlParam
     *            filter definition in RSQL syntax
     *
     * @return a paged result of all meta data entries for a given software
     *         module id
     *
     * @throws RSQLParameterUnsupportedFieldException
     *             if a field in the RSQL string is used but not provided by the
     *             given {@code fieldNameProvider}
     *
     * @throws RSQLParameterSyntaxException
     *             if the RSQL syntax is wrong
     *
     * @throws EntityNotFoundException
     *             if software module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<SoftwareModuleMetadata> findMetaDataByRsql(@NotNull Pageable pageable, long id,
            @NotNull String rsqlParam);

    /**
     * Retrieves the {@link SoftwareModule}s by their {@link SoftwareModuleType}
     * .
     *
     * @param pageable
     *            page parameters
     * @param typeId
     *            to be filtered on
     *
     * @return the found {@link SoftwareModule}s
     * @throws EntityNotFoundException if software module type with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Slice<SoftwareModule> findByType(@NotNull Pageable pageable, long typeId);

    /**
     * Locks a software module.
     *
     * @param id the software module id
     * @throws EntityNotFoundException if software module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    void lock(long id);

    /**
     * Unlocks a software module.<br/>
     * Use it with extreme care! In general once software module is locked
     * it shall not be unlocked. Note that it could have been assigned / deployed to targets.
     *
     * @param id the software module id
     * @throws EntityNotFoundException if software module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    void unlock(long id);

    /**
     * Updates a distribution set meta data value if corresponding entry exists.
     *
     * @param update
     *            the meta data entry to be updated
     *
     * @return the updated meta data entry
     *
     * @throws EntityNotFoundException
     *             in case the meta data entry does not exists and cannot be
     *             updated
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    SoftwareModuleMetadata updateMetaData(@NotNull @Valid SoftwareModuleMetadataUpdate update);
}
