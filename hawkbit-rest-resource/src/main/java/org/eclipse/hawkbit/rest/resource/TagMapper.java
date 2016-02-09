/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.rest.resource.api.DistributionSetTagRestApi;
import org.eclipse.hawkbit.rest.resource.api.TargetTagRestApi;
import org.eclipse.hawkbit.rest.resource.model.tag.TagRequestBodyPut;
import org.eclipse.hawkbit.rest.resource.model.tag.TagRest;
import org.eclipse.hawkbit.rest.resource.model.tag.TagsRest;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 */
final class TagMapper {
    private TagMapper() {
        // Utility class
    }

    static TagsRest toResponse(final List<TargetTag> targetTags) {
        final TagsRest tagsRest = new TagsRest();
        if (targetTags == null) {
            return tagsRest;
        }

        for (final TargetTag target : targetTags) {
            final TagRest response = toResponse(target);

            tagsRest.add(response);
        }
        return tagsRest;
    }

    static TagRest toResponse(final TargetTag targetTag) {
        final TagRest response = new TagRest();
        if (targetTag == null) {
            return response;
        }

        mapTag(response, targetTag);

        response.add(linkTo(methodOn(TargetTagRestApi.class).getTargetTag(targetTag.getId())).withRel("self"));

        response.add(linkTo(methodOn(TargetTagRestApi.class).getAssignedTargets(targetTag.getId()))
                .withRel("assignedTargets"));

        return response;
    }

    static TagsRest toResponseDistributionSetTag(final List<DistributionSetTag> distributionSetTags) {
        final TagsRest tagsRest = new TagsRest();
        if (distributionSetTags == null) {
            return tagsRest;
        }

        for (final DistributionSetTag distributionSetTag : distributionSetTags) {
            final TagRest response = toResponse(distributionSetTag);

            tagsRest.add(response);
        }
        return tagsRest;
    }

    static TagRest toResponse(final DistributionSetTag distributionSetTag) {
        final TagRest response = new TagRest();
        if (distributionSetTag == null) {
            return null;
        }

        mapTag(response, distributionSetTag);

        response.add(linkTo(methodOn(DistributionSetTagRestApi.class).getDistributionSetTag(distributionSetTag.getId()))
                .withRel("self"));

        response.add(linkTo(
                methodOn(DistributionSetTagRestApi.class).getAssignedDistributionSets(distributionSetTag.getId()))
                        .withRel("assignedDistributionSets"));

        return response;
    }

    static List<TargetTag> mapTargeTagFromRequest(final Iterable<TagRequestBodyPut> tags) {
        final List<TargetTag> mappedList = new ArrayList<>();
        for (final TagRequestBodyPut targetTagRest : tags) {
            mappedList.add(
                    new TargetTag(targetTagRest.getName(), targetTagRest.getDescription(), targetTagRest.getColour()));
        }
        return mappedList;
    }

    static List<DistributionSetTag> mapDistributionSetTagFromRequest(final Iterable<TagRequestBodyPut> tags) {
        final List<DistributionSetTag> mappedList = new ArrayList<>();
        for (final TagRequestBodyPut targetTagRest : tags) {
            mappedList.add(new DistributionSetTag(targetTagRest.getName(), targetTagRest.getDescription(),
                    targetTagRest.getColour()));
        }
        return mappedList;
    }

    private static void mapTag(final TagRest response, final Tag tag) {
        RestModelMapper.mapNamedToNamed(response, tag);
        response.setTagId(tag.getId());
        response.setColour(tag.getColour());
    }

    static void updateTag(final TagRequestBodyPut response, final Tag tag) {
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
