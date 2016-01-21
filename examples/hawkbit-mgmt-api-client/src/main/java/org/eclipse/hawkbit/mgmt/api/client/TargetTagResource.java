/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.api.client;

import java.util.List;

import org.eclipse.hawkbit.rest.resource.RestConstants;
import org.eclipse.hawkbit.rest.resource.model.tag.AssignedTargetRequestBody;
import org.eclipse.hawkbit.rest.resource.model.tag.TagRequestBodyPut;
import org.eclipse.hawkbit.rest.resource.model.tag.TagRest;
import org.eclipse.hawkbit.rest.resource.model.tag.TagsRest;
import org.eclipse.hawkbit.rest.resource.model.tag.TargetTagAssigmentResultRest;
import org.eclipse.hawkbit.rest.resource.model.target.TargetsRest;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

/**
 * Client binding for the Target resource of the management API.
 */
public interface TargetTagResource {

    /**
     * Retrieves a single target tag based on the given ID.
     *
     * @param targetTagId
     *            the ID of the target tag to retrieve
     * @return a deserialized java bean containing the attributes of the
     *         returned target tag
     */
    @RequestLine("GET " + RestConstants.TARGET_TAG_V1_REQUEST_MAPPING + "/{targetTagId}")
    TagRest getTargetTag(@Param("targetTagId") Long targetTagId);

    /**
     * Creates a list of target tags.
     * 
     * @param tags
     *            the tags to be created
     * @return the created tag list
     */
    @RequestLine("POST " + RestConstants.TARGET_TAG_V1_REQUEST_MAPPING)
    @Headers("Content-Type: application/json")
    TagsRest createTargetTag(List<TagRequestBodyPut> tags);

    /**
     * Update attributes of a target tag.
     * 
     * @param targetTagId
     *            the target tag id to be updated
     * @param tag
     *            the request body
     * @return the updated target tag
     */
    @RequestLine("PUT " + RestConstants.TARGET_TAG_V1_REQUEST_MAPPING + "/{targetTagId}")
    @Headers("Content-Type: application/json")
    TagRest updateTagretTag(@Param("targetTagId") Long targetTagId, TagRequestBodyPut tag);

    /**
     * Deletes given target tag on given ID.
     *
     * @param targetTagId
     *            to be deleted
     */
    @RequestLine("DELETE " + RestConstants.TARGET_TAG_V1_REQUEST_MAPPING + "/{targetTagId}")
    void deleteTargetTag(@Param("targetTagId") final Long targetTagId);

    /**
     * Retrieves a all assigned targets on the given target tag id.
     *
     * @param targetTagId
     *            the ID of the target tag to retrieve
     * @return a list of targets
     */
    @RequestLine("GET " + RestConstants.TARGET_TAG_V1_REQUEST_MAPPING
            + RestConstants.TARGET_TAG_TAGERTS_REQUEST_MAPPING)
    TargetsRest getAssignedTargets(@Param("targetTagId") final Long targetTagId);

    /**
     * Toggle the tag assignment all assigned targets will be unassigned and all
     * unassigned targets will be assigned.
     *
     * @param targetTagId
     *            the ID of the target tag to toggle
     * @param assignedTargetRequestBodies
     *            a list of controller ids
     * @return a list of assigned and unassigned targets
     */
    @RequestLine("POST " + RestConstants.TARGET_TAG_V1_REQUEST_MAPPING
            + RestConstants.TARGET_TAG_TAGERTS_REQUEST_MAPPING + "/toggleTagAssignment")
    @Headers("Content-Type: application/json")
    TargetTagAssigmentResultRest toggleTagAssignment(@Param("targetTagId") final Long targetTagId,
            final List<AssignedTargetRequestBody> assignedTargetRequestBodies);

    /**
     * Assign targets to a given target tag id.
     *
     * @param targetTagId
     *            the ID of the target tag to add the targets
     * @param assignedTargetRequestBodies
     *            a list of controller ids
     * @return a list of assigned targets
     */
    @RequestLine("POST " + RestConstants.TARGET_TAG_V1_REQUEST_MAPPING
            + RestConstants.TARGET_TAG_TAGERTS_REQUEST_MAPPING)
    @Headers("Content-Type: application/json")
    TargetsRest assignTargets(@Param("targetTagId") final Long targetTagId,
            final List<AssignedTargetRequestBody> assignedTargetRequestBodies);

    /**
     * Unassign targets to a given target tag id.
     *
     * @param targetTagId
     *            the ID of the target tag to add the targets
     */
    @RequestLine("DELETE " + RestConstants.TARGET_TAG_V1_REQUEST_MAPPING
            + RestConstants.TARGET_TAG_TAGERTS_REQUEST_MAPPING)
    void unassignTargets(@Param("targetTagId") final Long targetTagId);

    /**
     * Unassign one target to a given target tag id.
     *
     * @param targetTagId
     *            the ID of the target tag to add the targets param
     * @param controllerId
     *            the controller id
     */
    @RequestLine("DELETE " + RestConstants.TARGET_TAG_V1_REQUEST_MAPPING
            + RestConstants.TARGET_TAG_TAGERTS_REQUEST_MAPPING + "/{controllerId}")
    void unassignTarget(@Param("targetTagId") final Long targetTagId, @Param("controllerId") final String controllerId);
}
