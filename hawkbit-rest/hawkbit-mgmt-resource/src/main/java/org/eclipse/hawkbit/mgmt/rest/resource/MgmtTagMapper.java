/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTag;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTagRequestBodyPut;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTagRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetTagRestApi;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.rest.data.ResponseList;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 */
final class MgmtTagMapper {
    private MgmtTagMapper() {
        // Utility class
    }

    static List<MgmtTag> toResponse(final List<TargetTag> targetTags) {
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

    static MgmtTag toResponse(final TargetTag targetTag) {
        final MgmtTag response = new MgmtTag();
        if (targetTag == null) {
            return response;
        }

        mapTag(response, targetTag);

        response.add(linkTo(methodOn(MgmtTargetTagRestApi.class).getTargetTag(targetTag.getId())).withSelfRel());

        return response;
    }

    static void addLinks(final TargetTag targetTag, final MgmtTag response) {
        response.add(linkTo(methodOn(MgmtTargetTagRestApi.class).getAssignedTargets(targetTag.getId(),
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE, null, null))
                        .withRel("assignedTargets"));

    }

    static List<MgmtTag> toResponseDistributionSetTag(final List<DistributionSetTag> distributionSetTags) {
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

    static MgmtTag toResponse(final DistributionSetTag distributionSetTag) {
        final MgmtTag response = new MgmtTag();
        if (distributionSetTag == null) {
            return null;
        }

        mapTag(response, distributionSetTag);

        response.add(
                linkTo(methodOn(MgmtDistributionSetTagRestApi.class).getDistributionSetTag(distributionSetTag.getId()))
                        .withSelfRel());

        return response;
    }

    static void addLinks(final DistributionSetTag distributionSetTag, final MgmtTag response) {
        response.add(linkTo(methodOn(MgmtDistributionSetTagRestApi.class).getAssignedDistributionSets(
                distributionSetTag.getId(), MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE, null, null))
                        .withRel("assignedDistributionSets"));
    }

    static List<TagCreate> mapTagFromRequest(final EntityFactory entityFactory,
            final Collection<MgmtTagRequestBodyPut> tags) {
        return tags.stream()
                .map(tagRest -> entityFactory.tag().create().name(tagRest.getName())
                        .description(tagRest.getDescription()).colour(tagRest.getColour()))
                .collect(Collectors.toList());
    }

    private static void mapTag(final MgmtTag response, final Tag tag) {
        MgmtRestModelMapper.mapNamedToNamed(response, tag);
        response.setTagId(tag.getId());
        response.setColour(tag.getColour());
    }
}
