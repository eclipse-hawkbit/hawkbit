/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator.http;

import org.eclipse.hawkbit.simulator.DDISimulatedDevice;

import feign.Body;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

/**
 * A feign based controller resource interface declaration for
 * {@link DDISimulatedDevice}s using over HTTP.
 * 
 * @author Michael Hirsch
 *
 */
public interface ControllerResource {

    /**
     * The base poll URL for the devices to retrieve if there is an update
     * available.
     * 
     * @param tenant
     *            the tenant of the device
     * @param controllerId
     *            the ID of the device
     * @return the plain json response of the http request
     */
    @RequestLine("GET /{tenant}/controller/v1/{controllerId}")
    @Headers({ "Content-Type: application/json" })
    String get(@Param("tenant") final String tenant, @Param("controllerId") final String controllerId);

    /**
     * Retrieving the deployment job response from the hawkbit update server.
     * 
     * @param tenant
     *            the tenant for the simulated device
     * @param controllerId
     *            the ID of the device
     * @param actionId
     *            the ID of the action to retrieve
     * @return the json response of the http request
     */
    @RequestLine("GET /{tenant}/controller/v1/{controllerId}/deploymentBase/{actionId}")
    @Headers({ "Content-Type: application/json" })
    String getDeployment(@Param("tenant") final String tenant, @Param("controllerId") final String controllerId,
            @Param("actionId") final long actionId);

    /**
     * Post a success update feedback to the hawkbit update server
     * 
     * @param tenant
     *            the tenant of the device
     * @param controllerId
     *            the ID of the device
     * @param actionId
     *            the ID of the action to post feedback back
     */
    @RequestLine("POST /{tenant}/controller/v1/{controllerId}/deploymentBase/{actionId}/feedback")
    @Headers("Content-Type: application/json")
    @Body("%7B\"id\":{actionId},\"time\":\"20140511T121314\",\"status\":%7B\"execution\":\"closed\",\"result\":%7B\"finished\":\"success\",\"progress\":%7B%7D%7D%7D%7D")
    void postSuccessFeedback(@Param("tenant") final String tenant, @Param("controllerId") final String controllerId,
            @Param("actionId") final long actionId);

    /**
     * Post a failure update feedback to the hawkbit update server
     * 
     * @param tenant
     *            the tenant of the device
     * @param controllerId
     *            the ID of the device
     * @param actionId
     *            the ID of the action to post feedback back
     */
    @RequestLine("POST /{tenant}/controller/v1/{controllerId}/deploymentBase/{actionId}/feedback")
    @Headers("Content-Type: application/json")
    @Body("%7B\"id\":{actionId},\"time\":\"20140511T121314\",\"status\":%7B\"execution\":\"closed\",\"result\":%7B\"finished\":\"failure\",\"progress\":%7B%7D%7D%7D%7D")
    void postErrorFeedback(@Param("tenant") final String tenant, @Param("controllerId") final String controllerId,
            @Param("actionId") final long actionId);
}
