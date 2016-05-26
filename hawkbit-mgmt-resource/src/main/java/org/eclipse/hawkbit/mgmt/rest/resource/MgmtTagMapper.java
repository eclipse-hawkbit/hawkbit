/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTag;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTagRequestBodyPut;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTagRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetTagRestApi;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.TargetTag;

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
        return tagsRest;
    }

    static MgmtTag toResponse(final TargetTag targetTag) {
        final MgmtTag response = new MgmtTag();
        if (targetTag == null) {
            return response;
        }

        mapTag(response, targetTag);

        response.add(linkTo(methodOn(MgmtTargetTagRestApi.class).getTargetTag(targetTag.getId())).withRel("self"));

        response.add(linkTo(methodOn(MgmtTargetTagRestApi.class).getAssignedTargets(targetTag.getId()))
                .withRel("assignedTargets"));

        return response;
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
        return tagsRest;
    }

    static MgmtTag toResponse(final DistributionSetTag distributionSetTag) {
        final MgmtTag response = new MgmtTag();
        if (distributionSetTag == null) {
            return null;
        }

        mapTag(response, distributionSetTag);

        response.add(linkTo(methodOn(MgmtDistributionSetTagRestApi.class).getDistributionSetTag(distributionSetTag.getId()))
                .withRel("self"));

        response.add(linkTo(
                methodOn(MgmtDistributionSetTagRestApi.class).getAssignedDistributionSets(distributionSetTag.getId()))
                        .withRel("assignedDistributionSets"));

        return response;
    }

    static List<TargetTag> mapTargeTagFromRequest(final Iterable<MgmtTagRequestBodyPut> tags) {
        final List<TargetTag> mappedList = new ArrayList<>();
        for (final MgmtTagRequestBodyPut targetTagRest : tags) {
            mappedList.add(
                    new TargetTag(targetTagRest.getName(), targetTagRest.getDescription(), targetTagRest.getColour()));
        }
        return mappedList;
    }

    static List<DistributionSetTag> mapDistributionSetTagFromRequest(final Iterable<MgmtTagRequestBodyPut> tags) {
        final List<DistributionSetTag> mappedList = new ArrayList<>();
        for (final MgmtTagRequestBodyPut targetTagRest : tags) {
            mappedList.add(new DistributionSetTag(targetTagRest.getName(), targetTagRest.getDescription(),
                    targetTagRest.getColour()));
        }
        return mappedList;
    }

    private static void mapTag(final MgmtTag response, final Tag tag) {
        MgmtRestModelMapper.mapNamedToNamed(response, tag);
        response.setTagId(tag.getId());
        response.setColour(tag.getColour());
    }

    static void updateTag(final MgmtTagRequestBodyPut response, final Tag tag) {
        if (response.getDescription() != null) {
            tag.setDescription(response.getDescription());
        }

        if (response.getColour() != null) {
            tag.setColour(response.getColour());
        }

        if (response.getName() != null) {
            tag.setName(response.getName());
        }
    }
}
