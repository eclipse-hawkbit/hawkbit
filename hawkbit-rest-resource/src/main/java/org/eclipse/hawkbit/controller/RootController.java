/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.eclipse.hawkbit.ControllerPollProperties;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.cache.CacheWriteNotify;
import org.eclipse.hawkbit.controller.model.ActionFeedback;
import org.eclipse.hawkbit.controller.model.Cancel;
import org.eclipse.hawkbit.controller.model.CancelActionToStop;
import org.eclipse.hawkbit.controller.model.Chunk;
import org.eclipse.hawkbit.controller.model.ConfigData;
import org.eclipse.hawkbit.controller.model.ControllerBase;
import org.eclipse.hawkbit.controller.model.Deployment;
import org.eclipse.hawkbit.controller.model.Deployment.HandlingType;
import org.eclipse.hawkbit.controller.model.DeploymentBase;
import org.eclipse.hawkbit.controller.model.Result.FinalResult;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.rest.resource.helper.RestResourceConversionHelper;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.util.IpUtil;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The {@link RootController} of the SP server controller API that is queried by
 * the SP controller in order to pull {@link Action}s that have to be fullfilled
 * and report status updates concerning the {@link Action} processing.
 *
 * Transactional (read-write) as all queries at least update the last poll time.
 *
 *
 *
 *
 *
 */
@RestController
@RequestMapping(ControllerConstants.BASE_V1_REQUEST_MAPPING)
@Transactional
@Api(value = "/", description = "SP Direct Device Integration API")
public class RootController implements EnvironmentAware {

    private static final Logger LOG = LoggerFactory.getLogger(RootController.class);
    private static final String GIVEN_ACTION_IS_NOT_ASSIGNED_TO_GIVEN_TARGET = "given action ({}) is not assigned to given target ({}).";

    private static final String SP_SERVER_CONFIG_PREFIX = "hawkbit.server.";

    @Autowired
    private ControllerManagement controllerManagement;

    @Autowired
    private SoftwareManagement softwareManagement;

    @Autowired
    private ArtifactManagement artifactManagement;

    @Autowired
    private ControllerPollProperties controllerPollProperties;

    @Autowired
    private CacheWriteNotify cacheWriteNotify;

    @Autowired
    private TenantAware tenantAware;

    private String requestHeader;

    @Override
    public void setEnvironment(final Environment environment) {
        final RelaxedPropertyResolver relaxedPropertyResolver = new RelaxedPropertyResolver(environment,
                SP_SERVER_CONFIG_PREFIX);

        requestHeader = relaxedPropertyResolver.getProperty("security.rp.remote_ip_header", String.class,
                "X-Forwarded-For");
    }

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
    @ApiOperation(response = org.eclipse.hawkbit.controller.model.Artifact.class, value = "Returns artifacts of given software module", notes = "Returns all artifacts whichs is assigned to the software module")
    public ResponseEntity<List<org.eclipse.hawkbit.controller.model.Artifact>> getSoftwareModulesArtifacts(
            @PathVariable final String targetid, @PathVariable final Long softwareModuleId) {
        LOG.debug("getSoftwareModulesArtifacts({})", targetid);

        final SoftwareModule softwareModule = softwareManagement.findSoftwareModuleById(softwareModuleId);

        if (softwareModule == null) {
            LOG.warn("Software module with id {} could not be found.", softwareModuleId);
            throw new EntityNotFoundException("Software module does not exist");

        }

        return new ResponseEntity<>(DataConversionHelper.createArtifacts(targetid, softwareModule, tenantAware),
                HttpStatus.OK);
    }

    /**
     * Returns all available software modules for a given target.
     *
     * @param targetid
     *            of the {@link Target} that matches to
     *            {@link Target#getControllerId()}
     * @param request
     *            the HTTP request injected by spring
     * @return the response
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{targetid}/softwaremodules/", produces = {
            "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    @ApiOperation(response = SoftwareModule.class, value = " Returns software modules of given target", notes = "Returns all available software modules for a given target")
    public ResponseEntity<List<org.eclipse.hawkbit.controller.model.SoftwareModule>> getSoftwareModules(
            @PathVariable final String targetid, final HttpServletRequest request) {
        LOG.debug("getSoftwareModules({})", targetid);

        final Target target = controllerManagement.updateLastTargetQuery(targetid,
                IpUtil.getClientIpFromRequest(request, requestHeader));

        final DistributionSet assignedDistributionSet = target.getAssignedDistributionSet();

        if (assignedDistributionSet == null) {
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
        }

        return new ResponseEntity<>(DataConversionHelper.createSoftwareModules(targetid, assignedDistributionSet,
                tenantAware), HttpStatus.OK);
    }

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
    @ApiOperation(response = ControllerBase.class, value = "Controller base poll resource", notes = "This base resource can be regularly polled by the controller on the provisiong target or device "
            + "in order to retrieve actions that need to be executed. The resource supports Etag based modification "
            + "checks in order to save traffic.")
    public ResponseEntity<ControllerBase> getControllerBase(@PathVariable final String targetid,
            final HttpServletRequest request) {
        LOG.debug("getControllerBase({})", targetid);

        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotexist(targetid,
                IpUtil.getClientIpFromRequest(request, requestHeader));

        if (target.getTargetInfo().getUpdateStatus() == TargetUpdateStatus.UNKNOWN) {
            LOG.debug("target with {} extsisted but was in status UNKNOWN -> REGISTERED)", targetid);
            controllerManagement.updateTargetStatus(target.getTargetInfo(), TargetUpdateStatus.REGISTERED,
                    System.currentTimeMillis(), IpUtil.getClientIpFromRequest(request, requestHeader));
        }

        return new ResponseEntity<ControllerBase>(DataConversionHelper.fromTarget(target,
                controllerManagement.findActionByTargetAndActive(target), controllerPollProperties.getPollingTime(),
                tenantAware), HttpStatus.OK);
    }

    /**
     * Handles GET {@link Artifact} download request. This could be full or
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
    @ApiOperation(response = Void.class, value = "Downstream of given artifact", notes = "Download resource for artifacts. The resource supports partial download "
            + "as specified by RFC7233 (range requests). Keep in mind that the controller "
            + "needs to have the artifact assigned in order to be granted permission to download.")
    public ResponseEntity<Void> downloadArtifact(@PathVariable final String targetid,
            @PathVariable final Long softwareModuleId, @PathVariable final String fileName,
            final HttpServletResponse response, final HttpServletRequest request) {
        ResponseEntity<Void> result = null;

        final Target target = controllerManagement.updateLastTargetQuery(targetid,
                IpUtil.getClientIpFromRequest(request, requestHeader));
        final SoftwareModule module = softwareManagement.findSoftwareModuleById(softwareModuleId);

        if (checkModule(fileName, module)) {
            LOG.warn("Softare module with id {} could not be found.", softwareModuleId);
            result = new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            final LocalArtifact artifact = module.getLocalArtifactByFilename(fileName).get();
            final DbArtifact file = artifactManagement.loadLocalArtifactBinary(artifact);

            final String ifMatch = request.getHeader("If-Match");
            if (ifMatch != null && !RestResourceConversionHelper.matchesHttpHeader(ifMatch, artifact.getSha1Hash())) {
                result = new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
            } else {
                final Action action = checkAndLogDownload(request, target, module);
                result = RestResourceConversionHelper.writeFileResponse(artifact, response, request, file,
                        cacheWriteNotify, action.getId());
            }

        }

        return result;
    }

    private Action checkAndLogDownload(final HttpServletRequest request, final Target target,
            final SoftwareModule module) {
        final Action action = controllerManagement.getActionForDownloadByTargetAndSoftwareModule(
                target.getControllerId(), module);
        final String range = request.getHeader("Range");

        final ActionStatus statusMessage = new ActionStatus();
        statusMessage.setAction(action);
        statusMessage.setOccurredAt(System.currentTimeMillis());
        statusMessage.setStatus(Status.DOWNLOAD);

        if (range != null) {
            statusMessage.addMessage("It is a partial download request: " + range);
        } else {
            statusMessage.addMessage("Controller downloads");
        }
        controllerManagement.addActionStatusMessage(statusMessage);
        return action;
    }

    private boolean checkModule(final String fileName, final SoftwareModule module) {
        return null == module || !module.getLocalArtifactByFilename(fileName).isPresent();
    }

    /**
     * Handles GET {@link Artifact} MD5 checksum file download request.
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
            + ControllerConstants.ARTIFACT_MD5_DWNL_SUFFIX, produces = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(response = Void.class, value = "Downstream of given artifacts MD5SUM file", notes = "Download resource for MD5SUM file is an optional functionally especially usefull for "
            + "Linux based devices on order to check artifact coonsitency after download by using the md5sum "
            + "command line tool. The MD5 and SHA1 are in addition available as metadata in the deployment command itself.")
    public ResponseEntity<Void> downloadArtifactMd5(@PathVariable final String targetid,
            @PathVariable final Long softwareModuleId, @PathVariable final String fileName,
            final HttpServletResponse response, final HttpServletRequest request) {
        controllerManagement.updateLastTargetQuery(targetid, IpUtil.getClientIpFromRequest(request, requestHeader));

        final SoftwareModule module = softwareManagement.findSoftwareModuleById(softwareModuleId);

        if (checkModule(fileName, module)) {
            LOG.warn("Software module with id {} could not be found.", softwareModuleId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        try {
            DataConversionHelper.writeMD5FileResponse(fileName, response, module.getLocalArtifactByFilename(fileName)
                    .get());
        } catch (final IOException e) {
            LOG.error("Failed to stream MD5 File", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Resource for {@link SoftwareModule} {@link UpdateAction}s.
     *
     * @param targetid
     *            of the {@link Target} that matches to
     *            {@link Target#getControllerId()}
     * @param actionId
     *            of the {@link DeploymentBase} that matches to
     *            {@link Target#getActiveActions()}
     * @param resource
     *            an hashcode of the resource which indicates if the action has
     *            been changed, e.g. from 'soft' to 'force' and the eTag needs
     *            to be re-generated
     * @param request
     *            the HTTP request injected by spring
     * @return the response
     */
    @RequestMapping(value = "/{targetid}/" + ControllerConstants.DEPLOYMENT_BASE_ACTION + "/{actionId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(response = DeploymentBase.class, value = "Deployment or update action", notes = "Core resource for deployment operations. Contains all information necessary in order to execute the operation.")
    public ResponseEntity<DeploymentBase> getControllerBasedeploymentAction(
            @PathVariable @NotEmpty final String targetid, @PathVariable @NotEmpty final Long actionId,
            @RequestParam(value = "c", required = false, defaultValue = "-1") final int resource,
            final HttpServletRequest request) {
        LOG.debug("getControllerBasedeploymentAction({},{})", targetid, resource);

        final Target target = controllerManagement.updateLastTargetQuery(targetid,
                IpUtil.getClientIpFromRequest(request, requestHeader));

        final Action action = findActionWithExceptionIfNotFound(actionId);
        if (!action.getTarget().getId().equals(target.getId())) {
            LOG.warn(GIVEN_ACTION_IS_NOT_ASSIGNED_TO_GIVEN_TARGET, action.getId(), target.getId());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (!action.isCancelingOrCanceled()) {

            final List<Chunk> chunks = DataConversionHelper.createChunks(targetid, action, tenantAware);

            final HandlingType handlingType = action.isForce() ? HandlingType.FORCED : HandlingType.ATTEMPT;

            final DeploymentBase base = new DeploymentBase(Long.toString(action.getId()), new Deployment(handlingType,
                    handlingType, chunks));

            LOG.debug("Found an active UpdateAction for target {}. returning deyploment: {}", targetid, base);

            controllerManagement.registerRetrieved(action,
                    "Controller retrieved update action and should start now the download.");

            return new ResponseEntity<DeploymentBase>(base, HttpStatus.OK);
        }

        return new ResponseEntity<DeploymentBase>(HttpStatus.NOT_FOUND);
    }

    /**
     * This is the feedback channel for the {@link DeploymentBase} action.
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
    @RequestMapping(value = "/{targetid}/" + ControllerConstants.DEPLOYMENT_BASE_ACTION + "/{actionId}/"
            + ControllerConstants.FEEDBACK, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(response = ActionFeedback.class, value = "Feedback channel for update actions", notes = "Feedback channel. It is up to the device to decided how much intermediate feedback is "
            + "provided. However, the action will be kept open until the controller on the device reports a "
            + "finished (either successfull or error).")
    public ResponseEntity<ActionFeedback> postBasedeploymentActionFeedback(
            @Valid @RequestBody final ActionFeedback feedback, @PathVariable final String targetid,
            @PathVariable @NotEmpty final Long actionId, final HttpServletRequest request) {
        LOG.debug("provideBasedeploymentActionFeedback for target [{},{}]: {}", targetid, actionId, feedback);

        final Target target = controllerManagement.updateLastTargetQuery(targetid,
                IpUtil.getClientIpFromRequest(request, requestHeader));

        if (!actionId.equals(feedback.getId())) {
            LOG.warn(
                    "provideBasedeploymentActionFeedback: action in payload ({}) was not identical to action in path ({}).",
                    feedback.getId(), actionId);
            return new ResponseEntity<ActionFeedback>(HttpStatus.NOT_FOUND);
        }

        final Action action = findActionWithExceptionIfNotFound(actionId);
        if (!action.getTarget().getId().equals(target.getId())) {
            LOG.warn(GIVEN_ACTION_IS_NOT_ASSIGNED_TO_GIVEN_TARGET, action.getId(), target.getId());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (!action.isActive()) {
            LOG.warn("Updating action {} with feedback {} not possible since action not active anymore.",
                    action.getId(), feedback.getId());
            return new ResponseEntity<ActionFeedback>(HttpStatus.GONE);
        }

        controllerManagement.addUpdateActionStatus(generateUpdateStatus(feedback, targetid, feedback.getId(), action),
                action);

        return new ResponseEntity<ActionFeedback>(HttpStatus.OK);

    }

    private ActionStatus generateUpdateStatus(final ActionFeedback feedback, final String targetid,
            final Long actionid, final Action action) {

        final ActionStatus actionStatus = new ActionStatus();
        actionStatus.setAction(action);
        actionStatus.setOccurredAt(System.currentTimeMillis());

        switch (feedback.getStatus().getExecution()) {
        case CANCELED:
            LOG.debug("Controller confirmed cancel (actionid: {}, targetid: {}) as we got {} report.", actionid,
                    targetid, feedback.getStatus().getExecution());
            actionStatus.setStatus(Status.CANCELED);
            actionStatus.addMessage("Controller confirmed cancelation");
            break;
        case REJECTED:
            LOG.info("Controller reported internal error (actionid: {}, targetid: {}) as we got {} report.", actionid,
                    targetid, feedback.getStatus().getExecution());
            actionStatus.setStatus(Status.WARNING);
            actionStatus.addMessage("Controller reported internal ERROR and REJECTED update.");
            break;
        case CLOSED:
            LOG.debug("Controller reported closed (actionid: {}, targetid: {}) as we got {} report.", actionid,
                    targetid, feedback.getStatus().getExecution());
            if (feedback.getStatus().getResult().getFinished() == FinalResult.FAILURE) {
                actionStatus.setStatus(Status.ERROR);
                actionStatus.addMessage("Controller reported CLOSED with ERROR!");
            } else {
                actionStatus.setStatus(Status.FINISHED);
                actionStatus.addMessage("Controller reported CLOSED with OK!");
            }
            break;
        default:
            LOG.debug("Controller reported intermediate status (actionid: {}, targetid: {}) as we got {} report.",
                    actionid, targetid, feedback.getStatus().getExecution());
            actionStatus.setStatus(Status.RUNNING);
            // MECS-400: we should not use the unstructed message list for
            // the
            // server comment on the
            // status.
            actionStatus.addMessage("Controller reported: " + feedback.getStatus().getExecution());
            break;
        }

        action.setStatus(actionStatus.getStatus());

        if (feedback.getStatus().getDetails() != null && !feedback.getStatus().getDetails().isEmpty()) {
            final List<String> details = feedback.getStatus().getDetails();
            for (final String detailMsg : details) {
                actionStatus.addMessage(detailMsg);
            }
        }

        return actionStatus;
    }

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
    @RequestMapping(value = "/{targetid}/" + ControllerConstants.CONFIG_DATA_ACTION, method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(response = ConfigData.class, value = "Response to a requested metadata pull from the provisioning target device.", notes = "The usual behaviour is that when a new device resgisters at the server it is "
            + "requested to provide the meta information that will allow the server to identify the device on a "
            + "hardware level (e.g. hardware revision, mac address, serial number etc.).")
    public ResponseEntity<ConfigData> putConfigData(@Valid @RequestBody final ConfigData configData,
            @PathVariable final String targetid, final HttpServletRequest request) {
        controllerManagement.updateLastTargetQuery(targetid, IpUtil.getClientIpFromRequest(request, requestHeader));

        controllerManagement.updateControllerAttributes(targetid, configData.getData());

        return new ResponseEntity<ConfigData>(HttpStatus.OK);
    }

    /**
     * {@link RequestMethod.GET} method for the {@link Cancel} action.
     *
     * @param targetid
     *            ID of the calling target
     * @param actionId
     *            of the action
     * @param request
     *            the HTTP request injected by spring
     *
     * @return the {@link Cancel} response
     */
    @RequestMapping(value = "/{targetid}/" + ControllerConstants.CANCEL_ACTION + "/{actionId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(response = Cancel.class, value = "Cancel an action", notes = "The SP server might cancel an operation, e.g. an unfinished update has a sucessor. "
            + "It is up to the provisiong target to decide to accept the cancelation or reject it.")
    public ResponseEntity<Cancel> getControllerCancelAction(@PathVariable @NotEmpty final String targetid,
            @PathVariable @NotEmpty final Long actionId, final HttpServletRequest request) {
        LOG.debug("getControllerCancelAction({})", targetid);

        final Target target = controllerManagement.updateLastTargetQuery(targetid,
                IpUtil.getClientIpFromRequest(request, requestHeader));

        final Action action = findActionWithExceptionIfNotFound(actionId);
        if (!action.getTarget().getId().equals(target.getId())) {
            LOG.warn(GIVEN_ACTION_IS_NOT_ASSIGNED_TO_GIVEN_TARGET, action.getId(), target.getId());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (action.isCancelingOrCanceled()) {
            final Cancel cancel = new Cancel(String.valueOf(action.getId()), new CancelActionToStop(
                    String.valueOf(action.getId())));

            LOG.debug("Found an active CancelAction for target {}. returning cancel: {}", targetid, cancel);

            controllerManagement.registerRetrieved(action,
                    "Controller retrieved cancel action and should start now the cancelation.");

            return new ResponseEntity<Cancel>(cancel, HttpStatus.OK);
        }

        return new ResponseEntity<Cancel>(HttpStatus.NOT_FOUND);
    }

    /**
     * {@link RequestMethod.POST} method receiving the {@link ActionFeedback}
     * from the target.
     *
     * @param feedback
     *            the {@link ActionFeedback} from the target.
     * @param targetid
     *            the ID of the calling target
     * @param actionId
     *            of the action we have feedback for
     * @param request
     *            the HTTP request injected by spring
     *
     * @return the {@link ActionFeedback} response
     */
    @RequestMapping(value = "/{targetid}/" + ControllerConstants.CANCEL_ACTION + "/{actionId}/"
            + ControllerConstants.FEEDBACK, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(response = Cancel.class, value = "Feedback channel for cancel actions", notes = "It is up to the device to decided how much intermediate feedback is "
            + "provided. However, the action will be kept open until the controller on the device reports a "
            + "finished (either successfull or error) or rejects the oprtioan, e.g. the canceled actions have been started already.")
    public ResponseEntity<ActionFeedback> postCancelActionFeedback(@Valid @RequestBody final ActionFeedback feedback,
            @PathVariable @NotEmpty final String targetid, @PathVariable @NotEmpty final Long actionId,
            final HttpServletRequest request) {
        LOG.debug("provideCancelActionFeedback for target [{}]: {}", targetid, feedback);

        final Target target = controllerManagement.updateLastTargetQuery(targetid,
                IpUtil.getClientIpFromRequest(request, requestHeader));

        if (!actionId.equals(feedback.getId())) {
            LOG.warn(
                    "provideBasedeploymentActionFeedback: action in payload ({}) was not identical to action in path ({}).",
                    feedback.getId(), actionId);
            return new ResponseEntity<ActionFeedback>(HttpStatus.NOT_FOUND);
        }

        final Action action = findActionWithExceptionIfNotFound(actionId);
        if (!action.getTarget().getId().equals(target.getId())) {
            LOG.warn(GIVEN_ACTION_IS_NOT_ASSIGNED_TO_GIVEN_TARGET, action.getId(), target.getId());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        controllerManagement.addCancelActionStatus(
                generateActionCancelStatus(feedback, target, feedback.getId(), action), action);
        return new ResponseEntity<ActionFeedback>(HttpStatus.OK);
    }

    private static ActionStatus generateActionCancelStatus(final ActionFeedback feedback, final Target target,
            final Long actionid, final Action action) {

        final ActionStatus actionStatus = new ActionStatus();
        actionStatus.setAction(action);
        actionStatus.setOccurredAt(System.currentTimeMillis());

        switch (feedback.getStatus().getExecution()) {
        case CANCELED:
            LOG.error(
                    "Controller reported cancel for a cancel which is not supported by the server (actionid: {}, targetid: {}) as we got {} report.",
                    actionid, target.getControllerId(), feedback.getStatus().getExecution());
            actionStatus.setStatus(Status.WARNING);
            break;
        case REJECTED:
            LOG.info("Controller rejected the cancelation request (too late) (actionid: {}, targetid: {}).", actionid,
                    target.getControllerId());
            actionStatus.setStatus(Status.WARNING);
            break;
        case CLOSED:
            if (feedback.getStatus().getResult().getFinished() == FinalResult.FAILURE) {
                actionStatus.setStatus(Status.ERROR);
            } else {
                actionStatus.setStatus(Status.CANCELED);
            }
            break;
        default:
            actionStatus.setStatus(Status.RUNNING);
            break;
        }

        action.setStatus(actionStatus.getStatus());

        if (feedback.getStatus().getDetails() != null && !feedback.getStatus().getDetails().isEmpty()) {
            final List<String> details = feedback.getStatus().getDetails();
            for (final String detailMsg : details) {
                actionStatus.addMessage(detailMsg);
            }
        }

        return actionStatus;

    }

    private Action findActionWithExceptionIfNotFound(final Long actionId) {
        final Action findAction = controllerManagement.findActionWithDetails(actionId);
        if (findAction == null) {
            throw new EntityNotFoundException("Action with Id {" + actionId + "} does not exist");
        }
        return findAction;
    }

}
