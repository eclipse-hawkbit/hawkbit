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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTag;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTagRequestBodyPut;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTagRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetTagRestApi;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.rest.json.model.ResponseList;

/**
 * A mapper which maps repository model to RESTful model representation and back.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MgmtTagMapper {

    public static List<MgmtTag> toResponse(final List<? extends TargetTag> targetTags) {
        final List<MgmtTag> tagsRest = new ArrayList<>();
        if (targetTags == null) {
            return tagsRest;
        }

        for (final TargetTag target : targetTags) {
            final MgmtTag response = toResponse(target);

            tagsRest.add(response);
        }
        return new ResponseList<>(tagsRest);
    }

    public static MgmtTag toResponse(final TargetTag targetTag) {
        final MgmtTag response = new MgmtTag();
        if (targetTag == null) {
            return response;
        }

        mapTag(response, targetTag);

        response.add(linkTo(methodOn(MgmtTargetTagRestApi.class).getTargetTag(targetTag.getId())).withSelfRel().expand());

        return response;
    }

    public static void addLinks(final TargetTag targetTag, final MgmtTag response) {
        response.add(linkTo(methodOn(MgmtTargetTagRestApi.class).getAssignedTargets(targetTag.getId(),
                null, MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE, null)).withRel("assignedTargets")
                .expand());
    }

    public static List<MgmtTag> toResponseDistributionSetTag(final Collection<? extends DistributionSetTag> distributionSetTags) {
        final List<MgmtTag> tagsRest = new ArrayList<>();
        if (distributionSetTags == null) {
            return tagsRest;
        }

        for (final DistributionSetTag distributionSetTag : distributionSetTags) {
            final MgmtTag response = toResponse(distributionSetTag);

            tagsRest.add(response);
        }
        return new ResponseList<>(tagsRest);
    }

    public static MgmtTag toResponse(final DistributionSetTag distributionSetTag) {
        final MgmtTag response = new MgmtTag();
        if (distributionSetTag == null) {
            return null;
        }

        mapTag(response, distributionSetTag);

        response.add(
                linkTo(methodOn(MgmtDistributionSetTagRestApi.class).getDistributionSetTag(distributionSetTag.getId()))
                        .withSelfRel().expand());

        return response;
    }

    public static void addLinks(final DistributionSetTag distributionSetTag, final MgmtTag response) {
        response.add(linkTo(methodOn(MgmtDistributionSetTagRestApi.class).getAssignedDistributionSets(
                distributionSetTag.getId(), null, MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE, null))
                .withRel("assignedDistributionSets").expand());
    }

    public static List<TargetTagManagement.Create> mapTagFromRequest(final Collection<MgmtTagRequestBodyPut> tags) {
        return tags.stream()
                .map(tagRest -> TargetTagManagement.Create.builder()
                        .name(tagRest.getName())
                        .description(tagRest.getDescription())
                        .colour(tagRest.getColour())
                        .build())
                .map(TargetTagManagement.Create.class::cast)
                .toList();
    }

    private static void mapTag(final MgmtTag response, final Tag tag) {
        MgmtRestModelMapper.mapNamedToNamed(response, tag);
        response.setId(tag.getId());
        response.setColour(tag.getColour());
    }
}