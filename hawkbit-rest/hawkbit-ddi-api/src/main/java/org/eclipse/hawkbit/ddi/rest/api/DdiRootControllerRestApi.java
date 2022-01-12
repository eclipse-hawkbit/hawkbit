/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.api;

import java.io.InputStream;
import java.lang.annotation.Target;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import org.eclipse.hawkbit.ddi.json.model.DdiActionFeedback;
import org.eclipse.hawkbit.ddi.json.model.DdiArtifact;
import org.eclipse.hawkbit.ddi.json.model.DdiCancel;
import org.eclipse.hawkbit.ddi.json.model.DdiConfigData;
import org.eclipse.hawkbit.ddi.json.model.DdiControllerBase;
import org.eclipse.hawkbit.ddi.json.model.DdiDeploymentBase;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST resource handling for root controller CRUD operations.
 */
@RequestMapping(DdiRestConstants.BASE_V1_REQUEST_MAPPING)
public interface DdiRootControllerRestApi {

    /**
     * Returns all artifacts of a given software module and target.
     * 
     * @param tenant
     *            of the client
     * @param controllerId
     *            of the target that matches to controller id
     * @param softwareModuleId
     *            of the software module
     * @return the response
     */
    @GetMapping(value = "/{controllerId}/softwaremodules/{softwareModuleId}/artifacts", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE, DdiRestConstants.MEDIA_TYPE_CBOR })
    ResponseEntity<List<DdiArtifact>> getSoftwareModulesArtifacts(@PathVariable("tenant") final String tenant,
            @PathVariable("controllerId") final String controllerId,
            @PathVariable("softwareModuleId") final Long softwareModuleId);

    /**
     * Root resource for an individual {@link Target}.
     *
     * @param tenant
     *            of the request
     * @param controllerId
     *            of the target that matches to controller id
     * 
     * @return the response
     */
    @GetMapping(value = "/{controllerId}", produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE,
            DdiRestConstants.MEDIA_TYPE_CBOR })
    ResponseEntity<DdiControllerBase> getControllerBase(@PathVariable("tenant") final String tenant,
            @PathVariable("controllerId") final String controllerId);

    /**
     * Handles GET {@link DdiArtifact} download request. This could be full or
     * partial (as specified by RFC7233 (Range Requests)) download request.
     *
     * @param tenant
     *            of the request
     * @param controllerId
     *            of the target
     * @param softwareModuleId
     *            of the parent software module
     * @param fileName
     *            of the related local artifact
     *
     * @return response of the servlet which in case of success is status code
     *         {@link HttpStatus#OK} or in case of partial download
     *         {@link HttpStatus#PARTIAL_CONTENT}.
     */
    @GetMapping(value = "/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/{fileName}")
    ResponseEntity<InputStream> downloadArtifact(@PathVariable("tenant") final String tenant,
            @PathVariable("controllerId") final String controllerId,
            @PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("fileName") final String fileName);

    /**
     * Handles GET {@link DdiArtifact} MD5 checksum file download request.
     *
     * @param tenant
     *            of the request
     * @param controllerId
     *            of the target
     * @param softwareModuleId
     *            of the parent software module
     * @param fileName
     *            of the related local artifact
     *
     * @return {@link ResponseEntity} with status {@link HttpStatus#OK} if
     *         successful
     */
    @GetMapping(value = "/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/{fileName}"
            + DdiRestConstants.ARTIFACT_MD5_DWNL_SUFFIX, produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<Void> downloadArtifactMd5(@PathVariable("tenant") final String tenant,
            @PathVariable("controllerId") final String controllerId,
            @PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("fileName") final String fileName);

    /**
     * Resource for software module.
     *
     * @param tenant
     *            of the request
     * @param controllerId
     *            of the target
     * @param actionId
     *            of the {@link DdiDeploymentBase} that matches to active
     *            actions.
     * @param resource
     *            an hashcode of the resource which indicates if the action has
     *            been changed, e.g. from 'soft' to 'force' and the eTag needs
     *            to be re-generated
     * @param actionHistoryMessageCount
     *            specifies the number of messages to be returned from action
     *            history. Regardless of the passed value, in order to restrict
     *            resource utilization by controllers, maximum number of
     *            messages that are retrieved from database is limited by
     *            {@link RepositoryConstants#MAX_ACTION_HISTORY_MSG_COUNT}.
     * 
     *            actionHistoryMessageCount less than zero: retrieves the maximum
     *            allowed number of action status messages from history;
     * 
     *            actionHistoryMessageCount equal to zero: does not retrieve any
     *            message;
     * 
     *            actionHistoryMessageCount greater than zero: retrieves the
     *            specified number of messages, limited by maximum allowed number.
     * 
     * @return the response
     */
    @GetMapping(value = "/{controllerId}/" + DdiRestConstants.DEPLOYMENT_BASE_ACTION + "/{actionId}", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE, DdiRestConstants.MEDIA_TYPE_CBOR })
    ResponseEntity<DdiDeploymentBase> getControllerBasedeploymentAction(@PathVariable("tenant") final String tenant,
            @PathVariable("controllerId") @NotEmpty final String controllerId,
            @PathVariable("actionId") @NotEmpty final Long actionId,
            @RequestParam(value = "c", required = false, defaultValue = "-1") final int resource,
            @RequestParam(value = "actionHistory", defaultValue = DdiRestConstants.NO_ACTION_HISTORY) final Integer actionHistoryMessageCount);

    /**
     * This is the feedback channel for the {@link DdiDeploymentBase} action.
     *
     * @param tenant
     *            of the client
     * @param feedback
     *            to provide
     * @param controllerId
     *            of the target that matches to controller id
     * @param actionId
     *            of the action we have feedback for
     *
     * @return the response
     */
    @PostMapping(value = "/{controllerId}/" + DdiRestConstants.DEPLOYMENT_BASE_ACTION + "/{actionId}/"
            + DdiRestConstants.FEEDBACK, consumes = { MediaType.APPLICATION_JSON_VALUE,
                    DdiRestConstants.MEDIA_TYPE_CBOR })
    ResponseEntity<Void> postBasedeploymentActionFeedback(@Valid final DdiActionFeedback feedback,
            @PathVariable("tenant") final String tenant, @PathVariable("controllerId") final String controllerId,
            @PathVariable("actionId") @NotEmpty final Long actionId);

    /**
     * This is the feedback channel for the config data action.
     *
     * @param tenant
     *            of the client
     * @param configData
     *            as body
     * @param controllerId
     *            to provide data for
     *
     * @return status of the request
     */
    @PutMapping(value = "/{controllerId}/" + DdiRestConstants.CONFIG_DATA_ACTION, consumes = {
            MediaType.APPLICATION_JSON_VALUE, DdiRestConstants.MEDIA_TYPE_CBOR })
    ResponseEntity<Void> putConfigData(@Valid final DdiConfigData configData,
            @PathVariable("tenant") final String tenant, @PathVariable("controllerId") final String controllerId);

    /**
     * RequestMethod.GET method for the {@link DdiCancel} action.
     *
     * @param tenant
     *            of the request
     * @param controllerId
     *            ID of the calling target
     * @param actionId
     *            of the action
     *
     * @return the {@link DdiCancel} response
     */
    @GetMapping(value = "/{controllerId}/" + DdiRestConstants.CANCEL_ACTION + "/{actionId}", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE, DdiRestConstants.MEDIA_TYPE_CBOR })
    ResponseEntity<DdiCancel> getControllerCancelAction(@PathVariable("tenant") final String tenant,
            @PathVariable("controllerId") @NotEmpty final String controllerId,
            @PathVariable("actionId") @NotEmpty final Long actionId);

    /**
     * RequestMethod.POST method receiving the {@link DdiActionFeedback} from
     * the target.
     *
     * @param feedback
     *            the {@link DdiActionFeedback} from the target.
     * @param tenant
     *            of the client
     * @param controllerId
     *            the ID of the calling target
     * @param actionId
     *            of the action we have feedback for
     *
     * @return the {@link DdiActionFeedback} response
     */
    @PostMapping(value = "/{controllerId}/" + DdiRestConstants.CANCEL_ACTION + "/{actionId}/"
            + DdiRestConstants.FEEDBACK, consumes = { MediaType.APPLICATION_JSON_VALUE,
                    DdiRestConstants.MEDIA_TYPE_CBOR })
    ResponseEntity<Void> postCancelActionFeedback(@Valid final DdiActionFeedback feedback,
            @PathVariable("tenant") final String tenant,
            @PathVariable("controllerId") @NotEmpty final String controllerId,
            @PathVariable("actionId") @NotEmpty final Long actionId);

    /**
     * Resource for installed software module to retrieve the last successfully
     * finished action.
     *
     * @param tenant
     *            of the request
     * @param controllerId
     *            of the target
     * @param actionId
     *            of the {@link DdiDeploymentBase} that matches to installed action.
     * @param actionHistoryMessageCount
     *            specifies the number of messages to be returned from action
     *            history. Regardless of the passed value, in order to restrict
     *            resource utilization by controllers, maximum number of messages
     *            that are retrieved from database is limited by
     *            {@link RepositoryConstants#MAX_ACTION_HISTORY_MSG_COUNT}.
     * 
     *            actionHistoryMessageCount less than zero: retrieves the maximum
     *            allowed number of action status messages from history;
     * 
     *            actionHistoryMessageCount equal to zero: does not retrieve any
     *            message;
     * 
     *            actionHistoryMessageCount greater than zero: retrieves the
     *            specified number of messages, limited by maximum allowed number.
     * 
     * @return the {@link DdiDeploymentBase}. The response is of same format as for
     *         the /deploymentBase resource.
     */
    @GetMapping(value = "/{controllerId}/" + DdiRestConstants.INSTALLED_BASE_ACTION + "/{actionId}", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE, DdiRestConstants.MEDIA_TYPE_CBOR })
    ResponseEntity<DdiDeploymentBase> getControllerInstalledAction(@PathVariable("tenant") final String tenant,
            @PathVariable("controllerId") @NotEmpty final String controllerId,
            @PathVariable("actionId") @NotEmpty final Long actionId,
            @RequestParam(value = "actionHistory", defaultValue = DdiRestConstants.NO_ACTION_HISTORY) final Integer actionHistoryMessageCount);

}
