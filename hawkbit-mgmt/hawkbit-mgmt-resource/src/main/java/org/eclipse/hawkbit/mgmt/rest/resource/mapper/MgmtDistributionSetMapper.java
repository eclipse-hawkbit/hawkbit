/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource.mapper;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.mgmt.json.model.MgmtMetadata;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionId;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSetRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtTargetAssignmentResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTagRequestBodyPut;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.rest.json.model.ResponseList;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * A mapper which maps repository model to RESTful model representation and back.
 */
@Service
public class MgmtDistributionSetMapper {

    private final SoftwareModuleManagement<? extends SoftwareModule> softwareModuleManagement;

    MgmtDistributionSetMapper(final SoftwareModuleManagement<? extends SoftwareModule> softwareModuleManagement) {
        this.softwareModuleManagement = softwareModuleManagement;
    }

    public List<DistributionSetManagement.Create> fromRequest(
            final Collection<MgmtDistributionSetRequestBodyPost> sets,
            final String defaultDsKey, final Map<String, DistributionSetType> dsTypeKeyToDsType) {
        return sets.stream().<DistributionSetManagement.Create> map(dsRest -> {
            final Set<Long> modules = new HashSet<>();
            if (dsRest.getModules() != null) {
                dsRest.getModules().forEach(module -> modules.add(module.getId()));
            }
            // distribution set type, if null by the REST call shall be replaced with the default tenant DS  type
            final String dsTypeKey = Objects.requireNonNull(dsRest.getType(), "Distribution set type must not be null");
            final DistributionSetType dsType = dsTypeKeyToDsType.get(dsTypeKey);
            if (dsType == null) {
                // type should never null, cache is prefilled with all types
                throw new EntityNotFoundException(DistributionSetType.class, defaultDsKey);
            }
            return DistributionSetManagement.Create.builder()
                    .type(dsType)
                    .name(dsRest.getName()).version(dsRest.getVersion())
                    .description(dsRest.getDescription())
                    .modules(findSoftwareModuleWithExceptionIfNotFound(modules))
                    .requiredMigrationStep(dsRest.getRequiredMigrationStep())
                    .build();
        }).toList();
    }

    public static MgmtDistributionSet toResponse(final DistributionSet distributionSet) {
        if (distributionSet == null) {
            return null;
        }

        final MgmtDistributionSet response = new MgmtDistributionSet();
        MgmtRestModelMapper.mapNamedToNamed(response, distributionSet);

        response.setId(distributionSet.getId());
        response.setVersion(distributionSet.getVersion());
        response.setComplete(distributionSet.isComplete());
        response.setType(distributionSet.getType().getKey());
        response.setTypeName(distributionSet.getType().getName());
        response.setLocked(distributionSet.isLocked());
        response.setDeleted(distributionSet.isDeleted());
        response.setValid(distributionSet.isValid());

        distributionSet.getModules().forEach(module -> response.getModules().add(MgmtSoftwareModuleMapper.toResponse(module)));

        response.setRequiredMigrationStep(distributionSet.isRequiredMigrationStep());

        response.add(linkTo(methodOn(MgmtDistributionSetRestApi.class).getDistributionSet(response.getId())).withSelfRel().expand());

        return response;
    }

    public static void addLinks(final DistributionSet distributionSet, final MgmtDistributionSet response) {
        response.add(linkTo(methodOn(MgmtDistributionSetRestApi.class).getAssignedSoftwareModules(response.getId(),
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE, null))
                .withRel("modules").expand());

        response.add(linkTo(methodOn(MgmtDistributionSetTypeRestApi.class)
                .getDistributionSetType(distributionSet.getType().getId())).withRel("type").expand());

        response.add(linkTo(methodOn(MgmtDistributionSetRestApi.class).getMetadata(response.getId()))
                .withRel("metadata").expand());
    }

    public static MgmtTargetAssignmentResponseBody toResponse(final DistributionSetAssignmentResult dsAssignmentResult) {
        final MgmtTargetAssignmentResponseBody result = new MgmtTargetAssignmentResponseBody();
        result.setAlreadyAssigned(dsAssignmentResult.getAlreadyAssigned());
        result.setAssignedActions(dsAssignmentResult.getAssignedEntity().stream()
                .map(a -> new MgmtActionId(a.getTarget().getControllerId(), a.getId())).toList());
        return result;
    }

    public MgmtTargetAssignmentResponseBody toResponse(final List<DistributionSetAssignmentResult> dsAssignmentResults) {
        final MgmtTargetAssignmentResponseBody result = new MgmtTargetAssignmentResponseBody();
        final int alreadyAssigned = dsAssignmentResults.stream()
                .mapToInt(DistributionSetAssignmentResult::getAlreadyAssigned).sum();
        final List<MgmtActionId> assignedActions = dsAssignmentResults.stream()
                .flatMap(assignmentResult -> assignmentResult.getAssignedEntity().stream())
                .map(action -> new MgmtActionId(action.getTarget().getControllerId(), action.getId()))
                .toList();
        result.setAlreadyAssigned(alreadyAssigned);
        result.setAssignedActions(assignedActions);
        return result;
    }

    public static List<MgmtDistributionSet> toResponseDistributionSets(final Collection<? extends DistributionSet> sets) {
        if (sets == null) {
            return Collections.emptyList();
        }

        return new ResponseList<>(sets.stream().map(MgmtDistributionSetMapper::toResponse).toList());
    }

    public static List<DistributionSetTagManagement.Create> mapTagFromRequest(final Collection<MgmtTagRequestBodyPut> tags) {
        return tags.stream()
                .map(tagRest -> DistributionSetTagManagement.Create.builder()
                        .name(tagRest.getName())
                        .description(tagRest.getDescription()).colour(tagRest.getColour())
                        .build())
                .map(DistributionSetTagManagement.Create.class::cast)
                .toList();
    }

    public static MgmtMetadata toResponseDsMetadata(final String key, String value) {
        final MgmtMetadata metadataRest = new MgmtMetadata();
        metadataRest.setKey(key);
        metadataRest.setValue(value);
        return metadataRest;
    }

    public static Map<String, String> fromRequestDsMetadata(final List<MgmtMetadata> metadata) {
        return metadata == null
                ? Collections.emptyMap()
                : metadata.stream().collect(Collectors.toMap(MgmtMetadata::getKey, MgmtMetadata::getValue));
    }

    public static List<MgmtMetadata> toResponseDsMetadata(final Map<String, String> metadata) {
        return metadata.entrySet().stream().map(e -> toResponseDsMetadata(e.getKey(), e.getValue())).toList();
    }

    public static List<MgmtDistributionSet> toResponseFromDsList(final List<? extends DistributionSet> sets) {
        if (sets == null) {
            return Collections.emptyList();
        }

        return sets.stream().map(MgmtDistributionSetMapper::toResponse).toList();
    }

    private Set<? extends SoftwareModule> findSoftwareModuleWithExceptionIfNotFound(final Set<Long> softwareModuleIds) {
        if (CollectionUtils.isEmpty(softwareModuleIds)) {
            return Collections.emptySet();
        }

        final List<? extends SoftwareModule> modules = softwareModuleManagement.get(softwareModuleIds);
        if (modules.size() < softwareModuleIds.size()) {
            throw new EntityNotFoundException(SoftwareModule.class, softwareModuleIds);
        }

        return new HashSet<>(modules);
    }
}