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
import java.util.Map;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.eclipse.hawkbit.im.authentication.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataUpdate;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Type;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service for managing {@link SoftwareModule}s.
 */
public interface SoftwareModuleManagement<T extends SoftwareModule>
        extends RepositoryManagement<T, SoftwareModuleManagement.Create, SoftwareModuleManagement.Update> {

    @Override
    default String permissionGroup() {
        return "SOFTWARE_MODULE";
    }

    /**
     * Creates a list of software module meta-data entries.
     *
     * @param metadata the meta-data entries to create
     * @throws EntityAlreadyExistsException in case one of the meta-data entry already exists for the specific key
     * @throws EntityNotFoundException if software module with given ID does not exist
     * @throws AssignmentQuotaExceededException if the maximum number of {@link SoftwareModuleMetadata} entries is exceeded for the addressed
     *         {@link SoftwareModule}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    void createMetadata(@NotNull @Valid Collection<SoftwareModuleMetadataCreate> metadata);

    /**
     * Finds all meta-data by the given software module id.
     *
     * @param id the software module id to retrieve the meta-data from
     * @return a paged result of all meta-data entries for a given software module id
     * @throws EntityNotFoundException if software module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    List<SoftwareModuleMetadata> getMetadata(long id);

    /**
     * Finds all meta-data by the given software module id and key.
     *
     * @param id the software module id to retrieve the meta-data from
     * @param key the meta-data key to retrieve
     * @return a paged result of all meta-data entries for a given software module id
     * @throws EntityNotFoundException if software module with given ID does not exist ot the
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    SoftwareModuleMetadata getMetadata(long id, String key);

    /**
     * Finds all meta-data by the given software module id where {@link SoftwareModuleMetadata#isTargetVisible()}.
     *
     * @param id the software module id to retrieve the meta-data from
     * @param pageable the page request to page the result
     * @return a paged result of all meta-data entries for a given software module id
     * @throws EntityNotFoundException if software module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Page<SoftwareModuleMetadata> findMetaDataBySoftwareModuleIdAndTargetVisible(long id, @NotNull Pageable pageable);

    /**
     * Creates or updates a single software module meta-data entry.
     *
     * @param metadata the meta-data entry to create
     * @return the updated or created software module meta-data entry
     * @throws EntityAlreadyExistsException in case the meta-data entry already exists for the specific key
     * @throws EntityNotFoundException if software module with given ID does not exist
     * @throws AssignmentQuotaExceededException if the maximum number of {@link SoftwareModuleMetadata}
     *         entries is exceeded for the addressed {@link SoftwareModule}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    SoftwareModuleMetadata updateMetadata(@NotNull @Valid SoftwareModuleMetadataCreate metadata);

    /**
     * Updates a distribution set meta-data value if corresponding entry exists.
     *
     * @param update the meta-data entry to be updated
     * @return the updated meta-data entry
     * @throws EntityNotFoundException in case the meta-data entry does not exist and cannot be updated
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    SoftwareModuleMetadata updateMetadata(@NotNull @Valid SoftwareModuleMetadataUpdate update);

    /**
     * Deletes a software module meta-data entry.
     *
     * @param id where meta-data has to be deleted
     * @param key of the meta-data element
     * @return true if really deleted, false if not
     * @throws EntityNotFoundException if software module with given ID does not exist or the key is not found
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    void deleteMetadata(long id, @NotEmpty String key);

    /**
     * Locks a software module.
     *
     * @param id the software module id
     * @throws EntityNotFoundException if software module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    void lock(long id);

    /**
     * Unlocks a software module.<br/>
     * Use it with extreme care! In general once software module is locked
     * it shall not be unlocked. Note that it could have been assigned / deployed to targets.
     *
     * @param id the software module id
     * @throws EntityNotFoundException if software module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    void unlock(long id);

    /**
     * Returns all modules assigned to given {@link DistributionSet}.
     *
     * @param distributionSetId to search for
     * @param pageable the page request to page the result set
     * @return all {@link SoftwareModule}s that are assigned to given {@link DistributionSet}.
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Page<T> findByAssignedTo(long distributionSetId, @NotNull Pageable pageable);

    /**
     * Filter {@link SoftwareModule}s with given {@link SoftwareModule#getName()} or {@link SoftwareModule#getVersion()}
     * and {@link SoftwareModule#getType()} that are not marked as deleted.
     *
     * @param searchText to be filtered as "like" on {@link SoftwareModule#getName()}
     * @param typeId to be filtered as "like" on {@link SoftwareModule#getType()}
     * @param pageable page parameter
     * @return the page of found {@link SoftwareModule}
     * @throws EntityNotFoundException if given software module type does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Slice<T> findByTextAndType(String searchText, Long typeId, @NotNull Pageable pageable);

    /**
     * Retrieves {@link SoftwareModule} by their name AND version AND type.
     *
     * @param name of the {@link SoftwareModule}
     * @param version of the {@link SoftwareModule}
     * @param typeId of the {@link SoftwareModule}
     * @return the found {@link SoftwareModule}
     * @throws EntityNotFoundException if software module type with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Optional<T> findByNameAndVersionAndType(@NotEmpty String name, @NotEmpty String version, long typeId);

    /**
     * Retrieves the {@link SoftwareModule}s by their {@link SoftwareModuleType}
     *
     * @param typeId to be filtered on
     * @param pageable page parameters
     * @return the found {@link SoftwareModule}s
     * @throws EntityNotFoundException if software module type with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Slice<T> findByType(long typeId, @NotNull Pageable pageable);

    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Map<Long, List<SoftwareModuleMetadata>> findMetaDataBySoftwareModuleIdsAndTargetVisible(Collection<Long> moduleIds);

    /**
     * Returns count of all modules assigned to given {@link DistributionSet}.
     *
     * @param distributionSetId to search for
     * @return count of {@link SoftwareModule}s that are assigned to given {@link DistributionSet}.
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    long countByAssignedTo(long distributionSetId);

    @SuperBuilder
    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    final class Create extends UpdateCreate {

        private boolean encrypted;
    }

    @SuperBuilder
    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    final class Update extends UpdateCreate implements Identifiable<Long> {

        @NotNull
        private Long id;
        @Builder.Default
        private Boolean locked = false;
    }

    @SuperBuilder
    @Getter
    class UpdateCreate {

        @ValidString
        @Size(min = 1, max = NamedEntity.NAME_MAX_SIZE)
        @NotNull(groups = Create.class)
        private String name;
        @ValidString
        @Size(min = 1, max = NamedVersionedEntity.VERSION_MAX_SIZE)
        @NotNull(groups = Create.class)
        private String version;
        @ValidString
        @Size(max = NamedEntity.DESCRIPTION_MAX_SIZE)
        private String description;
        @ValidString
        @Size(max = SoftwareModule.VENDOR_MAX_SIZE)
        private String vendor;
        private SoftwareModuleType type;
    }
}