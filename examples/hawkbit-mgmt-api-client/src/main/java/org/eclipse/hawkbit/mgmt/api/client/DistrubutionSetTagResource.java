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
import org.eclipse.hawkbit.rest.resource.model.distributionset.DistributionSetsRest;
import org.eclipse.hawkbit.rest.resource.model.tag.AssignedDistributionSetRequestBody;
import org.eclipse.hawkbit.rest.resource.model.tag.DistributionSetTagAssigmentResultRest;
import org.eclipse.hawkbit.rest.resource.model.tag.TagRequestBodyPut;
import org.eclipse.hawkbit.rest.resource.model.tag.TagRest;
import org.eclipse.hawkbit.rest.resource.model.tag.TagsRest;
import org.eclipse.hawkbit.rest.resource.model.target.TargetsRest;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

/**
 * Client binding for the DistributionSetTag resource of the management API.
 */
public interface DistrubutionSetTagResource {

    /**
     * Retrieves a single distributionset tag based on the given ID.
     *
     * @param dsTagId
     *            the ID of the distributionset tag to retrieve
     * @return a deserialized java bean containing the attributes of the
     *         returned distributionset tag
     */
    @RequestLine("GET " + RestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING + "/{dsTagId}")
    TagRest getDistributionSetTag(@Param("dsTagId") Long dsTagId);

    /**
     * Creates a list of distributionset tags.
     * 
     * @param tags
     *            the tags to be created
     * @return the created tag list
     */
    @RequestLine("POST " + RestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING)
    @Headers("Content-Type: application/json")
    TagsRest createDistributionSetTags(List<TagRequestBodyPut> tags);

    /**
     * Update attributes of a distributionset tag.
     * 
     * @param dsTagId
     *            the distributionset tag id to be updated
     * @param tag
     *            the request body
     * @return the updated distributionset tag
     */
    @RequestLine("PUT " + RestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING + "/{dsTagId}")
    @Headers("Content-Type: application/json")
    TagRest updateDistributionSetTag(@Param("dsTagId") Long dsTagId, TagRequestBodyPut tag);

    /**
     * Deletes given distributionset tag on given ID.
     *
     * @param dsTagId
     *            to be deleted
     */
    @RequestLine("DELETE " + RestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING + "/{dsTagId}")
    void deleteDistributionSetTag(@Param("dsTagId") final Long dsTagId);

    /**
     * Retrieves a all assigned targets on the given distributionset tag id.
     *
     * @param dsTagId
     *            the ID of the distributionset tag to retrieve
     * @return a list of targets
     */
    @RequestLine("GET " + RestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING
            + RestConstants.DISTRIBUTIONSET_REQUEST_MAPPING)
    DistributionSetsRest getAssignedDistributionSets(@Param("dsTagId") final Long dsTagId);

    /**
     * Toggle the tag assignment all assigned targets will be unassigned and all
     * unassigned targets will be assigned.
     *
     * @param dsTagId
     *            the ID of the distributionset tag to toggle
     * @param assignedTargetRequestBodies
     *            a list of controller ids
     * @return a list of assigned and unassigned targets
     */
    @RequestLine("POST " + RestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING
            + RestConstants.DISTRIBUTIONSET_REQUEST_MAPPING + "/toggleTagAssignment")
    @Headers("Content-Type: application/json")
    DistributionSetTagAssigmentResultRest toggleTagAssignment(@Param("dsTagId") final Long dsTagId,
            final List<AssignedDistributionSetRequestBody> assignedTargetRequestBodies);

    /**
     * Assign targets to a given distributionset tag id.
     *
     * @param dsTagId
     *            the ID of the distributionset tag to add the targets
     * @param assignedTargetRequestBodies
     *            a list of controller ids
     * @return a list of assigned targets
     */
    @RequestLine("POST " + RestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING
            + RestConstants.DISTRIBUTIONSET_REQUEST_MAPPING)
    @Headers("Content-Type: application/json")
    TargetsRest assignDistributionSets(@Param("dsTagId") final Long dsTagId,
            final List<AssignedDistributionSetRequestBody> assignedTargetRequestBodies);

    /**
     * Unassign targets to a given distributionset tag id.
     *
     * @param dsTagId
     *            the ID of the distributionset tag to add the targets
     */
    @RequestLine("DELETE " + RestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING
            + RestConstants.DISTRIBUTIONSET_REQUEST_MAPPING)
    void unassignDistributionSets(@Param("dsTagId") final Long dsTagId);

    /**
     * Unassign one target to a given distributionset tag id.
     *
     * @param dsTagId
     *            the ID of the distributionset tag to add the targets param
     * @param dsId
     *            the distributionset id
     */
    @RequestLine("DELETE " + RestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING
            + RestConstants.DISTRIBUTIONSET_REQUEST_MAPPING + "/{dsId}")
    void unassignDistributionSet(@Param("dsTagId") final Long dsTagId, @Param("dsId") final Long dsId);
}
