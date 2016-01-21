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

import org.eclipse.hawkbit.rest.resource.model.target.TargetPagedList;
import org.eclipse.hawkbit.rest.resource.model.target.TargetRequestBody;
import org.eclipse.hawkbit.rest.resource.model.target.TargetRest;
import org.eclipse.hawkbit.rest.resource.model.target.TargetsRest;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

/**
 * Client binding for the Target resource of the management API.
 */
public interface TargetResource {

    /**
     * Retrieves a single target based on the given ID.
     *
     * @param targetId
     *            the ID of the target to retrieve
     * @return a deserialized java bean containing the attributes of the
     *         returned target
     */
    @RequestLine("GET /rest/v1/targets/{targetId}")
    TargetRest getTarget(@Param("targetId") final String targetId);

    /**
     * Paged query of targets resource.
     *
     * @param pagingOffsetParam
     *            of the paged query
     * @param pagingLimitParam
     *            of the paged query
     * @return paged list of target entries
     */
    @RequestLine("GET /rest/v1/targets?offset={pagingOffsetParam}&limit={pagingLimitParam}")
    TargetPagedList getTargets(@Param("pagingOffsetParam") int pagingOffsetParam,
            @Param("pagingLimitParam") int pagingLimitParam);

    /**
     * Paged query of targets resource with default offset and limit.
     *
     * @return paged list of target entries
     */
    @RequestLine("GET /rest/v1/targets")
    TargetPagedList getTargets();

    /**
     * Deletes given target based on given ID.
     *
     * @param targetId
     *            to be deleted
     */
    @RequestLine("DELETE /rest/v1/targets/{targetId}")
    void deleteTarget(@Param("targetId") final String targetId);

    /**
     * Creates a list of targets.
     *
     * @param targets
     *            the request body java bean containing the necessary attributes
     *            for creating a target.
     * @return the list of targets which have been created
     */
    @RequestLine("POST /rest/v1/targets/")
    @Headers("Content-Type: application/json")
    TargetsRest createTargets(List<TargetRequestBody> targets);

}
