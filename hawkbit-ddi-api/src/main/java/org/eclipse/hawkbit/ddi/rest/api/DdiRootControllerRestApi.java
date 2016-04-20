/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ddi.rest.api;

import java.lang.annotation.Target;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.eclipse.hawkbit.ddi.json.model.DdiActionFeedback;
import org.eclipse.hawkbit.ddi.json.model.DdiArtifact;
import org.eclipse.hawkbit.ddi.json.model.DdiCancel;
import org.eclipse.hawkbit.ddi.json.model.DdiConfigData;
import org.eclipse.hawkbit.ddi.json.model.DdiControllerBase;
import org.eclipse.hawkbit.ddi.json.model.DdiDeploymentBase;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST resource handling for root controller CRUD operations.
 */
@RequestMapping(DdiRestConstants.BASE_V1_REQUEST_MAPPING)
public interface DdiRootControllerRestApi {

    /**
     * Returns all artifacts of a given software module and target.
     *
     * @param targetid
     *            of the {@link Target} that matches to
     *            {@link Target#getControllerId()}
     * @param softwareModuleId
     *            of the {@link SoftwareModule}
     * @return the response
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{targetid}/softwaremodules/{softwareModuleId}/artifacts", produces = {
            "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<org.eclipse.hawkbit.ddi.json.model.DdiArtifact>> getSoftwareModulesArtifacts(
            @PathVariable("targetid") final String targetid,
            @PathVariable("softwareModuleId") final Long softwareModuleId);

    /**
     * Root resource for an individual {@link Target}.
     *
     * @param targetid
     *            of the {@link Target} that matches to
     *            {@link Target#getControllerId()}
     * @param request
     *            the HTTP request injected by spring
     * @return the response
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{targetid}", produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<DdiControllerBase> getControllerBase(@PathVariable("targetid") final String targetid,
            final HttpServletRequest request);

    /**
     * Handles GET {@link DdiArtifact} download request. This could be full or
     * partial (as specified by RFC7233 (Range Requests)) download request.
     *
     * @param targetid
     *            of the related
     * @param softwareModuleId
     *            of the parent {@link SoftwareModule}
     * @param fileName
     *            of the related {@link LocalArtifact}
     * @param response
     *            of the servlet
     * @param request
     *            from the client
     *
     * @return response of the servlet which in case of success is status code
     *         {@link HttpStatus#OK} or in case of partial download
     *         {@link HttpStatus#PARTIAL_CONTENT}.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{targetid}/softwaremodules/{softwareModuleId}/artifacts/{fileName}")
    ResponseEntity<Void> downloadArtifact(@PathVariable("targetid") final String targetid,
            @PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("fileName") final String fileName, final HttpServletResponse response,
            final HttpServletRequest request);

    /**
     * Handles GET {@link DdiArtifact} MD5 checksum file download request.
     *
     * @param targetid
     *            of the related
     * @param softwareModuleId
     *            of the parent {@link SoftwareModule}
     * @param fileName
     *            of the related {@link LocalArtifact}
     * @param response
     *            of the servlet
     * @param request
     *            the HTTP request injected by spring
     *
     * @return {@link ResponseEntity} with status {@link HttpStatus#OK} if
     *         successful
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{targetid}/softwaremodules/{softwareModuleId}/artifacts/{fileName}"
            + DdiRestConstants.ARTIFACT_MD5_DWNL_SUFFIX, produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<Void> downloadArtifactMd5(@PathVariable("targetid") final String targetid,
            @PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("fileName") final String fileName, final HttpServletResponse response,
            final HttpServletRequest request);

    /**
     * Resource for {@link SoftwareModule} {@link UpdateAction}s.
     *
     * @param targetid
     *            of the {@link Target} that matches to
     *            {@link Target#getControllerId()}
     * @param actionId
     *            of the {@link DdiDeploymentBase} that matches to
     *            {@link Target#getActiveActions()}
     * @param resource
     *            an hashcode of the resource which indicates if the action has
     *            been changed, e.g. from 'soft' to 'force' and the eTag needs
     *            to be re-generated
     * @param request
     *            the HTTP request injected by spring
     * @return the response
     */
    @RequestMapping(value = "/{targetid}/" + DdiRestConstants.DEPLOYMENT_BASE_ACTION
            + "/{actionId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DdiDeploymentBase> getControllerBasedeploymentAction(
            @PathVariable("targetid") @NotEmpty final String targetid,
            @PathVariable("actionId") @NotEmpty final Long actionId,
            @RequestParam(value = "c", required = false, defaultValue = "-1") final int resource,
            final HttpServletRequest request);

    /**
     * This is the feedback channel for the {@link DdiDeploymentBase} action.
     *
     * @param feedback
     *            to provide
     * @param targetid
     *            of the {@link Target} that matches to
     *            {@link Target#getControllerId()}
     * @param actionId
     *            of the action we have feedback for
     * @param request
     *            the HTTP request injected by spring
     *
     * @return the response
     */
    @RequestMapping(value = "/{targetid}/" + DdiRestConstants.DEPLOYMENT_BASE_ACTION + "/{actionId}/"
            + DdiRestConstants.FEEDBACK, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> postBasedeploymentActionFeedback(@Valid @RequestBody final DdiActionFeedback feedback,
            @PathVariable("targetid") final String targetid, @PathVariable("actionId") @NotEmpty final Long actionId,
            final HttpServletRequest request);

    /**
     * This is the feedback channel for the config data action.
     *
     * @param configData
     *            as body
     * @param targetid
     *            to provide data for
     * @param request
     *            the HTTP request injected by spring
     *
     * @return status of the request
     */
    @RequestMapping(value = "/{targetid}/"
            + DdiRestConstants.CONFIG_DATA_ACTION, method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> putConfigData(@Valid @RequestBody final DdiConfigData configData,
            @PathVariable("targetid") final String targetid, final HttpServletRequest request);

    /**
     * {@link RequestMethod.GET} method for the {@link DdiCancel} action.
     *
     * @param targetid
     *            ID of the calling target
     * @param actionId
     *            of the action
     * @param request
     *            the HTTP request injected by spring
     *
     * @return the {@link DdiCancel} response
     */
    @RequestMapping(value = "/{targetid}/" + DdiRestConstants.CANCEL_ACTION
            + "/{actionId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DdiCancel> getControllerCancelAction(@PathVariable("targetid") @NotEmpty final String targetid,
            @PathVariable("actionId") @NotEmpty final Long actionId, final HttpServletRequest request);

    /**
     * {@link RequestMethod.POST} method receiving the {@link DdiActionFeedback}
     * from the target.
     *
     * @param feedback
     *            the {@link DdiActionFeedback} from the target.
     * @param targetid
     *            the ID of the calling target
     * @param actionId
     *            of the action we have feedback for
     * @param request
     *            the HTTP request injected by spring
     *
     * @return the {@link DdiActionFeedback} response
     */

    @RequestMapping(value = "/{targetid}/" + DdiRestConstants.CANCEL_ACTION + "/{actionId}/"
            + DdiRestConstants.FEEDBACK, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> postCancelActionFeedback(@Valid @RequestBody final DdiActionFeedback feedback,
            @PathVariable("targetid") @NotEmpty final String targetid,
            @PathVariable("actionId") @NotEmpty final Long actionId, final HttpServletRequest request);

}
