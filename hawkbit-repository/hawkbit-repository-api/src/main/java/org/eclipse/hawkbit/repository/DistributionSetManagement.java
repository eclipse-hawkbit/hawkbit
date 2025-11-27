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

import static org.eclipse.hawkbit.auth.SpringEvalExpressions.HAS_READ_REPOSITORY;
import static org.eclipse.hawkbit.auth.SpringEvalExpressions.HAS_UPDATE_REPOSITORY;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InvalidDistributionSetException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.exception.UnsupportedSoftwareModuleForThisDistributionSetException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Statistic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for {@link DistributionSet}s.
 */
public interface DistributionSetManagement<T extends DistributionSet>
        extends RepositoryManagement<T, DistributionSetManagement.Create, DistributionSetManagement.Update>, MetadataSupport<String> {

    @Override
    default String permissionGroup() {
        return SpPermission.DISTRIBUTION_SET;
    }

    /**
     * Find {@link DistributionSet} based on given ID including (lazy loaded) details, e.g. {@link DistributionSet#getModules()}. <br/>
     * For performance reasons it is recommended to use {@link #find(long)} if the details are not required.
     *
     * @param id to look for.
     * @return {@link DistributionSet}
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    T getWithDetails(long id);

    @PreAuthorize(HAS_READ_REPOSITORY)
    boolean shouldLockImplicitly(final DistributionSet distributionSet);

    /**
     * Locks a distribution set. From then on its functional properties could not be changed, and it could be assigned to targets
     *
     * @param distributionSet the distribution set
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(HAS_UPDATE_REPOSITORY)
    T lock(final DistributionSet distributionSet);

    /**
     * Unlocks a distribution set.<br/>
     * Use it with extreme care! In general once distribution set is locked it shall not be unlocked. Note that it could have been assigned /
     * deployed to targets.
     *
     * @param distributionSet the distribution set
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(HAS_UPDATE_REPOSITORY)
    T unlock(final DistributionSet distributionSet);

    /**
     * Sets the specified {@link DistributionSet} as invalidated.
     *
     * @param distributionSet the ID of the {@link DistributionSet} to be set to invalid
     */
    @PreAuthorize(HAS_UPDATE_REPOSITORY)
    T invalidate(DistributionSet distributionSet);

    /**
     * Assigns {@link SoftwareModule} to existing {@link DistributionSet}.
     *
     * @param id to assign and update
     * @param moduleIds to get assigned
     * @return the updated {@link DistributionSet}.
     * @throws EntityNotFoundException if (at least one) given module does not exist
     * @throws EntityReadOnlyException if tries to change the {@link DistributionSet} s while the DS is already in use.
     * @throws UnsupportedSoftwareModuleForThisDistributionSetException if {@link SoftwareModule#getType()} is not supported by this
     *         {@link DistributionSet#getType()}.
     * @throws AssignmentQuotaExceededException if the maximum number of {@link SoftwareModule}s is exceeded for the addressed
     *         {@link DistributionSet}.
     */
    @PreAuthorize(HAS_UPDATE_REPOSITORY)
    T assignSoftwareModules(long id, @NotEmpty Collection<Long> moduleIds);

    /**
     * Unassigns a {@link SoftwareModule} form an existing {@link DistributionSet}.
     *
     * @param id to get unassigned form
     * @param moduleId to be unassigned
     * @return the updated {@link DistributionSet}.
     * @throws EntityNotFoundException if given module or DS does not exist
     * @throws EntityReadOnlyException if use tries to change the {@link DistributionSet} s while the DS is already in use.
     */
    @PreAuthorize(HAS_UPDATE_REPOSITORY)
    T unassignSoftwareModule(long id, long moduleId);

    /**
     * Assign a {@link DistributionSetTag} assignment to given {@link DistributionSet}s.
     *
     * @param ids to assign for
     * @param tagId to assign
     * @return list of assigned ds
     * @throws EntityNotFoundException if tag with given ID does not exist or (at least one) of the distribution sets.
     */
    @PreAuthorize(HAS_UPDATE_REPOSITORY)
    List<T> assignTag(@NotEmpty Collection<Long> ids, long tagId);

    /**
     * Unassign a {@link DistributionSetTag} assignment to given {@link DistributionSet}s.
     *
     * @param ids to assign for
     * @param tagId to assign
     * @return list of assigned ds
     * @throws EntityNotFoundException if tag with given ID does not exist or (at least one) of the distribution sets.
     */
    @PreAuthorize(HAS_UPDATE_REPOSITORY)
    List<T> unassignTag(@NotEmpty Collection<Long> ids, long tagId);

    /**
     * Find distribution set by id and throw an exception if it is deleted, incomplete or invalidated.
     *
     * @param id id of {@link DistributionSet}
     * @return the found valid {@link DistributionSet}
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     * @throws InvalidDistributionSetException if distribution set with given ID is invalidated
     * @throws IncompleteDistributionSetException if distribution set with given ID is incomplete
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    T getValidAndComplete(long id);

    /**
     * Find distribution set by name and version.
     *
     * @param distributionName name of {@link DistributionSet}; case insensitive
     * @param version version of {@link DistributionSet}
     * @return the page with the found {@link DistributionSet}
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    T findByNameAndVersion(@NotEmpty String distributionName, @NotEmpty String version);

    /**
     * Retrieves {@link DistributionSet}s by filtering on the given parameters.
     *
     * @param tagId of the tag the DS are assigned to
     * @param pageable page parameter
     * @return the page of found {@link DistributionSet}
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *         given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     * @throws EntityNotFoundException of distribution set tag with given ID does not exist
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    Page<T> findByTag(long tagId, @NotNull Pageable pageable);

    /**
     * Retrieves {@link DistributionSet}s by filtering on the given parameters.
     *
     * @param rsql rsql query string
     * @param tagId of the tag the DS are assigned to
     * @param pageable page parameter
     * @return the page of found {@link DistributionSet}
     * @throws EntityNotFoundException of distribution set tag with given ID does not exist
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    Page<T> findByRsqlAndTag(@NotNull String rsql, long tagId, @NotNull Pageable pageable);

    /**
     * Count all {@link org.eclipse.hawkbit.repository.model.Rollout}s by status for
     * Distribution Set.
     *
     * @param id to look for
     * @return List of Statistics for {@link org.eclipse.hawkbit.repository.model.Rollout}s status counts
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    List<Statistic> countRolloutsByStatusForDistributionSet(@NotNull Long id);

    /**
     * Count all {@link org.eclipse.hawkbit.repository.model.Action}s by status for Distribution Set.
     *
     * @param id to look for
     * @return List of Statistics for {@link org.eclipse.hawkbit.repository.model.Action}s status counts
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    List<Statistic> countActionsByStatusForDistributionSet(@NotNull Long id);

    /**
     * Count all {@link TargetFilterQueryManagement.AutoAssignDistributionSetUpdate}s for Distribution Set.
     *
     * @param id to look for
     * @return number of {@link TargetFilterQueryManagement.AutoAssignDistributionSetUpdate}s
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    Long countAutoAssignmentsForDistributionSet(@NotNull Long id);

    @SuperBuilder
    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    final class Create extends UpdateCreate {

        @NotNull
        private DistributionSetType type;

        @Builder.Default
        private Set<? extends SoftwareModule> modules = Set.of();

        public Create setType(@NotEmpty DistributionSetType type) {
            this.type = Objects.requireNonNull(type, "type must not be null");
            return this;
        }

        @SuppressWarnings("java:S3400") // java:S3400 it's used via reflection
        public boolean isValid() {
            return true;
        }
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
        @Builder.Default
        private Boolean requiredMigrationStep = false;
    }
}