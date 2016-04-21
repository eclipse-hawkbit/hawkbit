/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.eclipse.hawkbit.api.ArtifactUrlHandler;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.cache.CacheWriteNotify;
import org.eclipse.hawkbit.ddi.json.model.DdiActionFeedback;
import org.eclipse.hawkbit.ddi.json.model.DdiCancel;
import org.eclipse.hawkbit.ddi.json.model.DdiCancelActionToStop;
import org.eclipse.hawkbit.ddi.json.model.DdiChunk;
import org.eclipse.hawkbit.ddi.json.model.DdiConfigData;
import org.eclipse.hawkbit.ddi.json.model.DdiControllerBase;
import org.eclipse.hawkbit.ddi.json.model.DdiDeployment;
import org.eclipse.hawkbit.ddi.json.model.DdiDeployment.HandlingType;
import org.eclipse.hawkbit.ddi.json.model.DdiDeploymentBase;
import org.eclipse.hawkbit.ddi.json.model.DdiResult.FinalResult;
import org.eclipse.hawkbit.ddi.rest.api.DdiRootControllerRestApi;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.rest.util.RestResourceConversionHelper;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.util.IpUtil;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The {@link DdiRootController} of the hawkBit server DDI API that is queried
 * by the hawkBit controller in order to pull {@link Action}s that have to be
 * fulfilled and report status updates concerning the {@link Action} processing.
 *
 * Transactional (read-write) as all queries at least update the last poll time.
 */
@RestController
public class DdiRootController implements DdiRootControllerRestApi {

    private static final Logger LOG = LoggerFactory.getLogger(DdiRootController.class);
    private static final String GIVEN_ACTION_IS_NOT_ASSIGNED_TO_GIVEN_TARGET = "given action ({}) is not assigned to given target ({}).";

    @Autowired
    private ControllerManagement controllerManagement;

    @Autowired
    private SoftwareManagement softwareManagement;

    @Autowired
    private ArtifactManagement artifactManagement;

    @Autowired
    private CacheWriteNotify cacheWriteNotify;

    @Autowired
    private HawkbitSecurityProperties securityProperties;

    @Autowired
    private TenantAware tenantAware;

    @Autowired
    private ArtifactUrlHandler artifactUrlHandler;

    @Override
    public ResponseEntity<List<org.eclipse.hawkbit.ddi.json.model.DdiArtifact>> getSoftwareModulesArtifacts(
            @PathVariable("targetid") final String targetid,
            @PathVariable("softwareModuleId") final Long softwareModuleId) {
        LOG.debug("getSoftwareModulesArtifacts({})", targetid);

        final SoftwareModule softwareModule = softwareManagement.findSoftwareModuleById(softwareModuleId);

        if (softwareModule == null) {
            LOG.warn("Software module with id {} could not be found.", softwareModuleId);
            throw new EntityNotFoundException("Software module does not exist");

        }

        return new ResponseEntity<>(DataConversionHelper.createArtifacts(targetid, softwareModule, artifactUrlHandler),
                HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DdiControllerBase> getControllerBase(@PathVariable("targetid") final String targetid,
            final HttpServletRequest request) {
        LOG.debug("getControllerBase({})", targetid);

        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotexist(targetid,
                IpUtil.getClientIpFromRequest(request, securityProperties.getClients().getRemoteIpHeader()));

        if (target.getTargetInfo().getUpdateStatus() == TargetUpdateStatus.UNKNOWN) {
            LOG.debug("target with {} extsisted but was in status UNKNOWN -> REGISTERED)", targetid);
            controllerManagement.updateTargetStatus(target.getTargetInfo(), TargetUpdateStatus.REGISTERED,
                    System.currentTimeMillis(),
                    IpUtil.getClientIpFromRequest(request, securityProperties.getClients().getRemoteIpHeader()));
        }

        return new ResponseEntity<>(
                DataConversionHelper.fromTarget(target, controllerManagement.findActionByTargetAndActive(target),
                        controllerManagement.findPollingTime(), tenantAware),
                HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> downloadArtifact(@PathVariable("targetid") final String targetid,
            @PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("fileName") final String fileName, final HttpServletResponse response,
            final HttpServletRequest request) {
        ResponseEntity<Void> result;

        final Target target = controllerManagement.updateLastTargetQuery(targetid,
                IpUtil.getClientIpFromRequest(request, securityProperties.getClients().getRemoteIpHeader()));
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
        final Action action = controllerManagement
                .getActionForDownloadByTargetAndSoftwareModule(target.getControllerId(), module);
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

    private static boolean checkModule(final String fileName, final SoftwareModule module) {
        return null == module || !module.getLocalArtifactByFilename(fileName).isPresent();
    }

    @Override
    public ResponseEntity<Void> downloadArtifactMd5(@PathVariable("targetid") final String targetid,
            @PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("fileName") final String fileName, final HttpServletResponse response,
            final HttpServletRequest request) {
        controllerManagement.updateLastTargetQuery(targetid,
                IpUtil.getClientIpFromRequest(request, securityProperties.getClients().getRemoteIpHeader()));

        final SoftwareModule module = softwareManagement.findSoftwareModuleById(softwareModuleId);

        if (checkModule(fileName, module)) {
            LOG.warn("Software module with id {} could not be found.", softwareModuleId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        try {
            DataConversionHelper.writeMD5FileResponse(fileName, response,
                    module.getLocalArtifactByFilename(fileName).get());
        } catch (final IOException e) {
            LOG.error("Failed to stream MD5 File", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DdiDeploymentBase> getControllerBasedeploymentAction(
            @PathVariable("targetid") final String targetid, @PathVariable("actionId") final Long actionId,
            @RequestParam(value = "c", required = false, defaultValue = "-1") final int resource,
            final HttpServletRequest request) {
        LOG.debug("getControllerBasedeploymentAction({},{})", targetid, resource);

        final Target target = controllerManagement.updateLastTargetQuery(targetid,
                IpUtil.getClientIpFromRequest(request, securityProperties.getClients().getRemoteIpHeader()));

        final Action action = findActionWithExceptionIfNotFound(actionId);
        if (!action.getTarget().getId().equals(target.getId())) {
            LOG.warn(GIVEN_ACTION_IS_NOT_ASSIGNED_TO_GIVEN_TARGET, action.getId(), target.getId());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (!action.isCancelingOrCanceled()) {

            final List<DdiChunk> chunks = DataConversionHelper.createChunks(targetid, action, artifactUrlHandler);

            final HandlingType handlingType = action.isForce() ? HandlingType.FORCED : HandlingType.ATTEMPT;

            final DdiDeploymentBase base = new DdiDeploymentBase(Long.toString(action.getId()),
                    new DdiDeployment(handlingType, handlingType, chunks));

            LOG.debug("Found an active UpdateAction for target {}. returning deyploment: {}", targetid, base);

            controllerManagement.registerRetrieved(action,
                    "Controller retrieved update action and should start now the download.");

            return new ResponseEntity<>(base, HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @Override
    public ResponseEntity<Void> postBasedeploymentActionFeedback(@Valid @RequestBody final DdiActionFeedback feedback,
            @PathVariable("targetid") final String targetid, @PathVariable("actionId") @NotEmpty final Long actionId,
            final HttpServletRequest request) {
        LOG.debug("provideBasedeploymentActionFeedback for target [{},{}]: {}", targetid, actionId, feedback);

        final Target target = controllerManagement.updateLastTargetQuery(targetid,
                IpUtil.getClientIpFromRequest(request, securityProperties.getClients().getRemoteIpHeader()));

        if (!actionId.equals(feedback.getId())) {
            LOG.warn(
                    "provideBasedeploymentActionFeedback: action in payload ({}) was not identical to action in path ({}).",
                    feedback.getId(), actionId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        final Action action = findActionWithExceptionIfNotFound(actionId);
        if (!action.getTarget().getId().equals(target.getId())) {
            LOG.warn(GIVEN_ACTION_IS_NOT_ASSIGNED_TO_GIVEN_TARGET, action.getId(), target.getId());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (!action.isActive()) {
            LOG.warn("Updating action {} with feedback {} not possible since action not active anymore.",
                    action.getId(), feedback.getId());
            return new ResponseEntity<>(HttpStatus.GONE);
        }

        controllerManagement.addUpdateActionStatus(

                generateUpdateStatus(feedback, targetid, feedback.getId(), action), action);

        return new ResponseEntity<>(HttpStatus.OK);

    }

    private ActionStatus generateUpdateStatus(final DdiActionFeedback feedback, final String targetid,
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
            handleClosedUpdateStatus(feedback, targetid, actionid, actionStatus);
            break;
        default:
            handleDefaultUpdateStatus(feedback, targetid, actionid, actionStatus);
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

    private static void handleDefaultUpdateStatus(final DdiActionFeedback feedback, final String targetid,
            final Long actionid, final ActionStatus actionStatus) {
        LOG.debug("Controller reported intermediate status (actionid: {}, targetid: {}) as we got {} report.", actionid,
                targetid, feedback.getStatus().getExecution());
        actionStatus.setStatus(Status.RUNNING);
        actionStatus.addMessage("Controller reported: " + feedback.getStatus().getExecution());
    }

    private static void handleClosedUpdateStatus(final DdiActionFeedback feedback, final String targetid,
            final Long actionid, final ActionStatus actionStatus) {
        LOG.debug("Controller reported closed (actionid: {}, targetid: {}) as we got {} report.", actionid, targetid,
                feedback.getStatus().getExecution());
        if (feedback.getStatus().getResult().getFinished() == FinalResult.FAILURE) {
            actionStatus.setStatus(Status.ERROR);
            actionStatus.addMessage("Controller reported CLOSED with ERROR!");
        } else {
            actionStatus.setStatus(Status.FINISHED);
            actionStatus.addMessage("Controller reported CLOSED with OK!");
        }
    }

    @Override
    public ResponseEntity<Void> putConfigData(@Valid @RequestBody final DdiConfigData configData,
            @PathVariable("targetid") final String targetid, final HttpServletRequest request) {
        controllerManagement.updateLastTargetQuery(targetid,
                IpUtil.getClientIpFromRequest(request, securityProperties.getClients().getRemoteIpHeader()));

        controllerManagement.updateControllerAttributes(targetid, configData.getData());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DdiCancel> getControllerCancelAction(
            @PathVariable("targetid") @NotEmpty final String targetid,
            @PathVariable("actionId") @NotEmpty final Long actionId, final HttpServletRequest request) {
        LOG.debug("getControllerCancelAction({})", targetid);

        final Target target = controllerManagement.updateLastTargetQuery(targetid,
                IpUtil.getClientIpFromRequest(request, securityProperties.getClients().getRemoteIpHeader()));

        final Action action = findActionWithExceptionIfNotFound(actionId);
        if (!action.getTarget().getId().equals(target.getId())) {
            LOG.warn(GIVEN_ACTION_IS_NOT_ASSIGNED_TO_GIVEN_TARGET, action.getId(), target.getId());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (action.isCancelingOrCanceled()) {
            final DdiCancel cancel = new DdiCancel(String.valueOf(action.getId()),
                    new DdiCancelActionToStop(String.valueOf(action.getId())));

            LOG.debug("Found an active CancelAction for target {}. returning cancel: {}", targetid, cancel);

            controllerManagement.registerRetrieved(action,
                    "Controller retrieved cancel action and should start now the cancelation.");

            return new ResponseEntity<>(cancel, HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @Override
    public ResponseEntity<Void> postCancelActionFeedback(@Valid @RequestBody final DdiActionFeedback feedback,
            @PathVariable("targetid") @NotEmpty final String targetid,
            @PathVariable("actionId") @NotEmpty final Long actionId, final HttpServletRequest request) {
        LOG.debug("provideCancelActionFeedback for target [{}]: {}", targetid, feedback);

        final Target target = controllerManagement.updateLastTargetQuery(targetid,
                IpUtil.getClientIpFromRequest(request, securityProperties.getClients().getRemoteIpHeader()));

        if (!actionId.equals(feedback.getId())) {
            LOG.warn(
                    "provideBasedeploymentActionFeedback: action in payload ({}) was not identical to action in path ({}).",
                    feedback.getId(), actionId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        final Action action = findActionWithExceptionIfNotFound(actionId);
        if (!action.getTarget().getId().equals(target.getId())) {
            LOG.warn(GIVEN_ACTION_IS_NOT_ASSIGNED_TO_GIVEN_TARGET, action.getId(), target.getId());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        controllerManagement
                .addCancelActionStatus(generateActionCancelStatus(feedback, target, feedback.getId(), action), action);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private static ActionStatus generateActionCancelStatus(final DdiActionFeedback feedback, final Target target,
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
            handleClosedCancelStatus(feedback, actionStatus);
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

    private static void handleClosedCancelStatus(final DdiActionFeedback feedback, final ActionStatus actionStatus) {
        if (feedback.getStatus().getResult().getFinished() == FinalResult.FAILURE) {
            actionStatus.setStatus(Status.ERROR);
        } else {
            actionStatus.setStatus(Status.CANCELED);
        }
    }

    private Action findActionWithExceptionIfNotFound(final Long actionId) {
        final Action findAction = controllerManagement.findActionWithDetails(actionId);
        if (findAction == null) {
            throw new EntityNotFoundException("Action with Id {" + actionId + "} does not exist");
        }
        return findAction;
    }
}
