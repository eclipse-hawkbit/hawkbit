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
import org.eclipse.hawkbit.rest.resource.model.distributionset.DistributionSetRequestBodyPost;
import org.eclipse.hawkbit.rest.resource.model.distributionset.DistributionSetsRest;

import feign.Headers;
import feign.RequestLine;

/**
 * Client binding for the Distribution resource of the management API.
 */
@FunctionalInterface
public interface DistributionSetResource {

    /**
     * Creates a list of distribution sets.
     *
     * @param sets
     *            the request body java bean containing the necessary attributes
     *            for creating a distribution set.
     * @return the list of targets which have been created
     */
    @RequestLine("POST " + RestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING)
    @Headers("Content-Type: application/json")
    DistributionSetsRest createDistributionSets(final List<DistributionSetRequestBodyPost> sets);

}
