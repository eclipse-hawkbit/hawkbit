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

import static org.eclipse.hawkbit.auth.SpringEvalExpressions.HAS_DELETE_REPOSITORY;
import static org.eclipse.hawkbit.auth.SpringEvalExpressions.HAS_READ_REPOSITORY;
import static org.eclipse.hawkbit.auth.SpringEvalExpressions.HAS_UPDATE_REPOSITORY;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.validation.Valid;
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
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.ObjectUtils;

/**
 * Management service for {@link Target}s.
 */
@SuppressWarnings("java:S1192") // java:S1192 nothing meaningful to add + would be interface constant
public interface TargetManagement<T extends Target>
        extends RepositoryManagement<T, TargetManagement.Create, TargetManagement.Update> {

    String HAS_READ_TARGET_AND_READ_ROLLOUT = HAS_READ_REPOSITORY + " and hasAuthority('READ_" + SpPermission.ROLLOUT + "')";
    String HAS_UPDATE_TARGET_AND_READ_ROLLOUT = HAS_UPDATE_REPOSITORY + " and hasAuthority('READ_" + SpPermission.ROLLOUT + "')";
    String HAS_READ_TARGET_AND_READ_DISTRIBUTION_SET = HAS_READ_REPOSITORY + " and hasAuthority('READ_" + SpPermission.DISTRIBUTION_SET + "')";
    String HAS_UPDATE_TARGET_AND_READ_DISTRIBUTION_SET = HAS_UPDATE_REPOSITORY + " and hasAuthority('READ_" + SpPermission.DISTRIBUTION_SET + "')";

    String DETAILS_AUTO_CONFIRMATION_STATUS = "autoConfirmationStatus";
    String DETAILS_TAGS = "tags";

    @Override
    default String permissionGroup() {
        return SpPermission.TARGET;
    }

    /**
     * Get controller attributes of given {@link Target}.
     *
     * @param controllerId of the target
     * @return controller attributes as key/value pairs
     * @throws EntityNotFoundException if target with given ID does not exist
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    Map<String, String> getControllerAttributes(@NotEmpty String controllerId);

    /**
     * Verify if a target matches a specific target filter query, does not have a
     * specific DS already assigned and is compatible with it.
     *
     * @param controllerId of the {@link org.eclipse.hawkbit.repository.model.Target} to check
     * @param distributionSetId of the {@link org.eclipse.hawkbit.repository.model.DistributionSet} to consider
     * @param targetFilterQuery to execute
     * @return true if it matches
     */
    @PreAuthorize(HAS_UPDATE_TARGET_AND_READ_DISTRIBUTION_SET)
    boolean isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(
            @NotNull String controllerId, long distributionSetId, @NotNull String targetFilterQuery);

    @PreAuthorize(HAS_READ_REPOSITORY)
    Target getByControllerId(@NotEmpty String controllerId);

    /**
     * Find a {@link Target} based a given ID.
     *
     * @param controllerId to look for.
     * @return {@link Target}
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    Optional<Target> findByControllerId(@NotEmpty String controllerId);

    /**
     * Find {@link Target}s based a given IDs.
     *
     * @param controllerIDs to look for.
     * @return List of found{@link Target}s
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    List<Target> findByControllerId(@NotEmpty Collection<String> controllerIDs);

    /**
     * Gets a {@link Target} based a given controller id and includes the details specified by the details key.
     *
     * @param controllerId to look for.
     * @param detailsKey the key of the details to include, e.g. {@link #DETAILS_AUTO_CONFIRMATION_STATUS}
     * @return {@link Target}
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    Target getWithDetails(@NotEmpty String controllerId, String detailsKey);

    @PreAuthorize(HAS_READ_REPOSITORY)
    default Target getWithAutoConfigurationStatus(@NotEmpty String controllerId) {
        return getWithDetails(controllerId, DETAILS_AUTO_CONFIRMATION_STATUS);
    }

    /**
     * Finds all targets for all the given parameter {@link TargetFilterQuery} and that don't have the specified distribution
     * set in their action history and are compatible with the passed {@link DistributionSetType}.
     *
     * @param distributionSetId id of the {@link DistributionSet}
     * @param rsql filter definition in RSQL syntax
     * @param pageable the pageable to enhance the query for paging and sorting
     * @return a page of the found {@link Target}s
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(HAS_UPDATE_TARGET_AND_READ_DISTRIBUTION_SET)
    Slice<Target> findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatable(
            long distributionSetId, @NotNull String rsql, @NotNull Pageable pageable);

    /**
     * Retrieves {@link Target}s by the assigned {@link DistributionSet}.
     *
     * @param distributionSetId the ID of the {@link DistributionSet}
     * @param pageable page parameter
     * @return the found {@link Target}s
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(HAS_READ_TARGET_AND_READ_DISTRIBUTION_SET)
    Page<Target> findByAssignedDistributionSet(long distributionSetId, @NotNull Pageable pageable);

    /**
     * Retrieves {@link Target}s by the assigned {@link DistributionSet} possible including additional filtering based on the given {@code spec}.
     *
     * @param distributionSetId the ID of the {@link DistributionSet}
     * @param rsql the specification to filter the result set
     * @param pageable page parameter
     * @return the found {@link Target}s, never {@code null}
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the given
     *         {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(HAS_READ_TARGET_AND_READ_DISTRIBUTION_SET)
    Page<Target> findByAssignedDistributionSetAndRsql(long distributionSetId, @NotNull String rsql, @NotNull Pageable pageable);

    /**
     * retrieves {@link Target}s by the installed {@link DistributionSet}.
     *
     * @param distributionSetId the ID of the {@link DistributionSet}
     * @param pageReq page parameter
     * @return the found {@link Target}s
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(HAS_READ_TARGET_AND_READ_DISTRIBUTION_SET)
    Page<Target> findByInstalledDistributionSet(long distributionSetId, @NotNull Pageable pageReq);

    /**
     * retrieves {@link Target}s by the installed {@link DistributionSet} including
     * additional filtering based on the given {@code spec}.
     *
     * @param distributionSetId the ID of the {@link DistributionSet}
     * @param rsql the specification to filter the result
     * @param pageReq page parameter
     * @return the found {@link Target}s, never {@code null}
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *         given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(HAS_READ_TARGET_AND_READ_DISTRIBUTION_SET)
    Page<Target> findByInstalledDistributionSetAndRsql(long distributionSetId, @NotNull String rsql, @NotNull Pageable pageReq);

    /**
     * Find targets by tag name.
     *
     * @param tagId tag ID
     * @param pageable the page request parameter for paging and sorting the result
     * @return list of matching targets
     * @throws EntityNotFoundException if target tag with given ID does not exist
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    Page<Target> findByTag(long tagId, @NotNull Pageable pageable);

    /**
     * Find targets by tag name.
     *
     * @param rsql in RSQL notation
     * @param tagId tag ID
     * @param pageable the page request parameter for paging and sorting the result
     * @return list of matching targets
     * @throws EntityNotFoundException if target tag with given ID does not exist
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *         given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    Page<Target> findByRsqlAndTag(@NotNull String rsql, long tagId, @NotNull Pageable pageable);

    /**
     * Count all targets for given {@link TargetFilterQuery} and that are compatible with the passed {@link DistributionSetType}.
     *
     * @param rsql filter definition in RSQL syntax
     * @param distributionSetIdTypeId ID of the {@link DistributionSetType} the targets need to be compatible with
     * @return the found number of{@link Target}s
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    long countByRsqlAndCompatible(@NotEmpty String rsql, @NotNull Long distributionSetIdTypeId);

    /**
     * Count all targets with failed actions for specific Rollout and that are compatible with the passed {@link DistributionSetType} and
     * created after given timestamp
     *
     * @param rolloutId rolloutId of the rollout to be retried.
     * @param dsTypeId ID of the {@link DistributionSetType} the targets need to be compatible with
     * @return the found number of{@link Target}s
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    long countByFailedInRollout(@NotEmpty String rolloutId, @NotNull Long dsTypeId);

    /**
     * Counts all targets for all the given parameter {@link TargetFilterQuery} and that don't have the specified distribution set in their
     * action history and are compatible with the passed {@link DistributionSetType}.
     *
     * @param distributionSetId id of the {@link DistributionSet}
     * @param rsql filter definition in RSQL syntax
     * @return the count of found {@link Target}s
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(HAS_UPDATE_TARGET_AND_READ_DISTRIBUTION_SET)
    long countByRsqlAndNonDsAndCompatibleAndUpdatable(long distributionSetId, @NotNull String rsql);

    /**
     * Deletes target with the given controller ID.
     *
     * @param controllerId the controller ID of the target to be deleted
     * @throws EntityNotFoundException if target with given ID does not exist
     */
    @PreAuthorize(HAS_DELETE_REPOSITORY)
    void deleteByControllerId(@NotEmpty String controllerId);

    /**
     * Assign a {@link TargetType} assignment to given {@link Target}.
     *
     * @param controllerId to un-assign for
     * @param targetTypeId Target type id
     * @return the unassigned target
     * @throws EntityNotFoundException if TargetType with given target ID does not exist
     */
    @PreAuthorize(HAS_UPDATE_REPOSITORY)
    Target assignType(@NotEmpty String controllerId, @NotNull Long targetTypeId);

    /**
     * Un-assign a {@link TargetType} assignment to given {@link Target}.
     *
     * @param controllerId to un-assign for
     * @return the unassigned target
     */
    @PreAuthorize(HAS_UPDATE_REPOSITORY)
    Target unassignType(@NotEmpty String controllerId);

    /**
     * Assign a {@link TargetTag} assignment to given {@link Target}s.
     *
     * @param controllerIds to assign for
     * @param targetTagId to assign
     * @param notFoundHandler if not all targets found - if null - exception, otherwise tag what found and the handler is called with what's not found
     * @return list of assigned targets
     * @throws EntityNotFoundException if given targetTagId or at least one of the targets do not exist
     */
    @PreAuthorize(HAS_UPDATE_REPOSITORY)
    List<Target> assignTag(@NotEmpty Collection<String> controllerIds, long targetTagId, final Consumer<Collection<String>> notFoundHandler);

    /**
     * Assign a {@link TargetTag} assignment to given {@link Target}s.
     *
     * @param controllerIds to assign for
     * @param targetTagId to assign
     * @return list of assigned targets
     * @throws EntityNotFoundException if given targetTagId or at least one of the targets do not exist
     */
    @PreAuthorize(HAS_UPDATE_REPOSITORY)
    List<Target> assignTag(@NotEmpty Collection<String> controllerIds, long targetTagId);

    /**
     * Finds a single target tags its id.
     *
     * @param controllerId of the {@link Target}
     * @return the found Tag set
     * @throws EntityNotFoundException if target with given ID does not exist
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    Set<TargetTag> getTags(@NotEmpty String controllerId);

    /**
     * Un-assign a {@link TargetTag} assignment to given {@link Target}s.
     *
     * @param controllerIds to un-assign for
     * @param targetTagId to un-assign
     * @param notFoundHandler if not all targets found - if null - exception, otherwise un-tag what found and the handler is called with what's not found
     * @return list of unassigned targets
     * @throws EntityNotFoundException if given targetTagId or at least one of the targets do not exist
     */
    @PreAuthorize(HAS_UPDATE_REPOSITORY)
    List<Target> unassignTag(@NotEmpty Collection<String> controllerIds, long targetTagId, final Consumer<Collection<String>> notFoundHandler);

    /**
     * Un-assign a {@link TargetTag} assignment to given {@link Target}s.
     *
     * @param controllerIds to un-assign for
     * @param targetTagId to un-assign
     * @return list of unassigned targets
     * @throws EntityNotFoundException if given targetTagId or at least one of the targets do not exist
     */
    @PreAuthorize(HAS_UPDATE_REPOSITORY)
    List<Target> unassignTag(@NotEmpty Collection<String> controllerIds, long targetTagId);

    /**
     * Assigns the target group of the targets matching the provided rsql filter.
     *
     * @param group target group parameter
     * @param rsql rsql filter for {@link Target}
     */
    @PreAuthorize(HAS_UPDATE_REPOSITORY)
    void assignTargetGroupWithRsql(String group, @NotNull String rsql);

    /**
     * Assigns the provided group to the targets which are in the provided list of controllerIds.
     *
     * @param group target group parameter
     * @param controllerIds list of targets
     */
    @PreAuthorize(HAS_UPDATE_REPOSITORY)
    void assignTargetsWithGroup(String group, @NotEmpty List<String> controllerIds);

    /**
     * Finds targets by group or subgroup.
     *
     * @param group - provided group/subgroup to filter for
     * @param withSubgroups - whether is a subgroup or not e.g. x/y/z
     * @param pageable - page parameter
     * @return all matching targets to provided group/subgroup
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    Page<Target> findTargetsByGroup(@NotEmpty String group, boolean withSubgroups, @NotNull Pageable pageable);

    /**
     * Finds all the distinct target groups in the scope of a tenant
     *
     * @return list of all distinct target groups
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    List<String> findGroups();

    /**
     * Creates or updates a meta-data value.
     *
     * @param controllerId the entity id which meta-data has to be updated
     * @param key the key of the meta-data entry to be updated
     * @param value the meta-data value to be updated
     * @throws EntityNotFoundException in case the meta-data entry does not exist and cannot be updated
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    void createMetadata(@NotNull String controllerId, @NotEmpty String key, @NotNull @Valid String value);

    /**
     * Creates a list of entity meta-data entries.
     *
     * @param controllerId the entity id which meta-data has to be created
     * @param metadata the meta-data entries to create
     * @throws EntityAlreadyExistsException in case one of the meta-data entry already exists for the specific key
     * @throws EntityNotFoundException if entity with given ID does not exist
     * @throws AssignmentQuotaExceededException if the maximum number of meta-data entries is exceeded for the addressed entity
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    void createMetadata(@NotNull String controllerId, @NotEmpty @Valid Map<String, String> metadata);

    /**
     * Finds all meta-data by the given entity id and key.
     *
     * @param controllerId the entity id to retrieve the meta-data from
     * @param key the meta-data key to retrieve
     * @return a paged result of all meta-data entries for a given entity id
     * @throws EntityNotFoundException if entity with given ID does not exist ot the
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    String getMetadata(@NotNull String controllerId, @NotEmpty String key);

    /**
     * Finds all meta-data by the given entity id.
     *
     * @param controllerId the entity id to retrieve the meta-data from
     * @return a paged result of all meta-data entries for a given entity id
     * @throws EntityNotFoundException if entity with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Map<String, String> getMetadata(@NotNull String controllerId);

    /**
     * Deletes a entity meta-data entry.
     *
     * @param controllerId where meta-data has to be deleted
     * @param key of the meta-data element
     * @throws EntityNotFoundException if entity with given ID does not exist or the key is not found
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    void deleteMetadata(@NotNull String controllerId, @NotEmpty String key);

    @SuperBuilder
    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    final class Create extends UpdateCreate {

        @ValidString
        @Size(min = 1, max = Target.CONTROLLER_ID_MAX_SIZE)
        @NotNull
        private String controllerId;

        @ValidString
        @Size(min = 1, max = NamedEntity.NAME_MAX_SIZE)
        @NotNull(groups = Create.class)
        private String name;

        @ValidString
        @Size(min = 1, max = Target.SECURITY_TOKEN_MAX_SIZE)
        @NotNull
        @ToString.Exclude
        private String securityToken;

        // java:S1144 - constructor is actually used by SuperBuilder's build() method
        // java:S3358 - better readable that way
        @SuppressWarnings({ "java:S1144", "java:S3358" })
        private Create(final CreateBuilder<?, ?> builder) {
            super(builder);
            controllerId = builder.controllerId;
            // truncate controller ID to max name length (if too big)
            name = ObjectUtils.isEmpty(builder.name)
                    ? controllerId != null && controllerId.length() > NamedEntity.NAME_MAX_SIZE
                    ? controllerId.substring(0, NamedEntity.NAME_MAX_SIZE)
                    : controllerId
                    : builder.name;
            securityToken = ObjectUtils.isEmpty(builder.securityToken)
                    ? SecurityTokenGeneratorHolder.getInstance().generateToken()
                    : builder.securityToken;
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

        @ValidString
        @Size(min = 1, max = Target.SECURITY_TOKEN_MAX_SIZE)
        @ToString.Exclude
        private String securityToken;

        private Boolean requestControllerAttributes;
    }

    @SuperBuilder
    @Getter
    class UpdateCreate {

        @ValidString
        @Size(max = NamedEntity.DESCRIPTION_MAX_SIZE)
        private String description;

        private TargetType targetType;

        @Size(max = Target.ADDRESS_MAX_SIZE)
        private String address;

        private Long lastTargetQuery;
        private TargetUpdateStatus updateStatus;

        @ValidString
        private String group;
    }
}