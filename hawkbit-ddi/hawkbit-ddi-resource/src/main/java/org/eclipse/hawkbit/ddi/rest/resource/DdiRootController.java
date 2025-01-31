/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.artifact.repository.urlhandler.ArtifactUrlHandler;
import org.eclipse.hawkbit.ddi.json.model.DdiActionFeedback;
import org.eclipse.hawkbit.ddi.json.model.DdiActionHistory;
import org.eclipse.hawkbit.ddi.json.model.DdiActivateAutoConfirmation;
import org.eclipse.hawkbit.ddi.json.model.DdiArtifact;
import org.eclipse.hawkbit.ddi.json.model.DdiAssignedVersion;
import org.eclipse.hawkbit.ddi.json.model.DdiAutoConfirmationState;
import org.eclipse.hawkbit.ddi.json.model.DdiCancel;
import org.eclipse.hawkbit.ddi.json.model.DdiCancelActionToStop;
import org.eclipse.hawkbit.ddi.json.model.DdiChunk;
import org.eclipse.hawkbit.ddi.json.model.DdiConfigData;
import org.eclipse.hawkbit.ddi.json.model.DdiConfirmationBase;
import org.eclipse.hawkbit.ddi.json.model.DdiConfirmationBaseAction;
import org.eclipse.hawkbit.ddi.json.model.DdiConfirmationFeedback;
import org.eclipse.hawkbit.ddi.json.model.DdiControllerBase;
import org.eclipse.hawkbit.ddi.json.model.DdiDeployment;
import org.eclipse.hawkbit.ddi.json.model.DdiDeployment.DdiMaintenanceWindowStatus;
import org.eclipse.hawkbit.ddi.json.model.DdiDeployment.HandlingType;
import org.eclipse.hawkbit.ddi.json.model.DdiDeploymentBase;
import org.eclipse.hawkbit.ddi.json.model.DdiResult.FinalResult;
import org.eclipse.hawkbit.ddi.json.model.DdiUpdateMode;
import org.eclipse.hawkbit.ddi.rest.api.DdiRestConstants;
import org.eclipse.hawkbit.ddi.rest.api.DdiRootControllerRestApi;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ConfirmationManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.UpdateMode;
import org.eclipse.hawkbit.repository.builder.ActionStatusCreate;
import org.eclipse.hawkbit.repository.event.remote.DownloadProgressEvent;
import org.eclipse.hawkbit.repository.exception.ArtifactBinaryNotFoundException;
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InvalidConfirmationFeedbackException;
import org.eclipse.hawkbit.repository.exception.SoftwareModuleNotAssignedToTargetException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.util.FileStreamingUtil;
import org.eclipse.hawkbit.rest.util.HttpUtil;
import org.eclipse.hawkbit.rest.util.RequestResponseContextHolder;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

/**
 * The {@link DdiRootController} of the hawkBit server DDI API that is queried by the hawkBit
 * controller in order to pull {@link Action}s that have to be fulfilled and report status updates concerning
 * the {@link Action} processing.
 *
 * Transactional (read-write) as all queries at least update the last poll time.
 */
@Slf4j
@RestController
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class DdiRootController implements DdiRootControllerRestApi {

    private static final String GIVEN_ACTION_IS_NOT_ASSIGNED_TO_GIVEN_TARGET = "given action ({}) is not assigned to given target ({}).";
    private static final String FALLBACK_REMARK = "Initiated using the Device Direct Integration API without providing a remark.";
    /**
     * File suffix for MDH hash download (see Linux md5sum).
     */
    private static final String ARTIFACT_MD5_DWNL_SUFFIX = ".MD5SUM";

    private final ControllerManagement controllerManagement;
    private final ConfirmationManagement confirmationManagement;
    private final ArtifactManagement artifactManagement;
    private final ArtifactUrlHandler artifactUrlHandler;
    private final SystemManagement systemManagement;
    private final ApplicationEventPublisher eventPublisher;
    private final BusProperties bus;
    private final HawkbitSecurityProperties securityProperties;
    private final TenantAware tenantAware;
    private final EntityFactory entityFactory;
    private ServiceMatcher serviceMatcher;

    @SuppressWarnings("java:S107")
    public DdiRootController(
            final ControllerManagement controllerManagement, final ConfirmationManagement confirmationManagement,
            final ArtifactManagement artifactManagement, final ArtifactUrlHandler artifactUrlHandler,
            final SystemManagement systemManagement,
            final ApplicationEventPublisher eventPublisher, final BusProperties bus,
            final HawkbitSecurityProperties securityProperties, final TenantAware tenantAware, final EntityFactory entityFactory) {
        this.controllerManagement = controllerManagement;
        this.confirmationManagement = confirmationManagement;
        this.artifactManagement = artifactManagement;
        this.artifactUrlHandler = artifactUrlHandler;
        this.systemManagement = systemManagement;
        this.eventPublisher = eventPublisher;
        this.bus = bus;
        this.securityProperties = securityProperties;
        this.tenantAware = tenantAware;
        this.entityFactory = entityFactory;
    }

    @Autowired(required = false)
    public void setServiceMatcher(final ServiceMatcher serviceMatcher) {
        this.serviceMatcher = serviceMatcher;
    }

    @Override
    public ResponseEntity<List<DdiArtifact>> getSoftwareModulesArtifacts(
            final String tenant,
            final String controllerId,
            final Long softwareModuleId) {
        log.debug("getSoftwareModulesArtifacts({})", controllerId);

        final Target target = findTarget(controllerId);

        final SoftwareModule softwareModule = controllerManagement.getSoftwareModule(softwareModuleId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, softwareModuleId));

        return new ResponseEntity<>(
                DataConversionHelper.createArtifacts(target, softwareModule, artifactUrlHandler, systemManagement,
                        new ServletServerHttpRequest(RequestResponseContextHolder.getHttpServletRequest())),
                HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DdiControllerBase> getControllerBase(final String tenant, final String controllerId) {
        log.debug("getControllerBase({})", controllerId);

        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotExist(controllerId, IpUtil
                .getClientIpFromRequest(RequestResponseContextHolder.getHttpServletRequest(), securityProperties));
        final Action activeAction = controllerManagement.findActiveActionWithHighestWeight(controllerId).orElse(null);

        final Action installedAction = controllerManagement.getInstalledActionByTarget(controllerId).orElse(null);

        checkAndCancelExpiredAction(activeAction);

        // activeAction
        return new ResponseEntity<>(DataConversionHelper.fromTarget(target, installedAction, activeAction,
                activeAction == null
                        ? controllerManagement.getPollingTime()
                        : controllerManagement.getPollingTimeForAction(activeAction.getId()), tenantAware),
                HttpStatus.OK);
    }

    @Override
    public ResponseEntity<InputStream> downloadArtifact(
            final String tenant,
            final String controllerId,
            final Long softwareModuleId,
            final String fileName) {
        final ResponseEntity<InputStream> result;

        final Target target = findTarget(controllerId);
        final SoftwareModule module = controllerManagement.getSoftwareModule(softwareModuleId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, softwareModuleId));

        if (checkModule(fileName, module)) {
            log.warn("Software module with id {} could not be found (1).", softwareModuleId);
            result = ResponseEntity.notFound().build();
        } else {
            // Artifact presence is ensured in 'checkModule'
            final Artifact artifact = module.getArtifactByFilename(fileName).orElseThrow(NoSuchElementException::new);
            final DbArtifact file = artifactManagement
                    .loadArtifactBinary(artifact.getSha1Hash(), module.getId(), module.isEncrypted())
                    .orElseThrow(() -> new ArtifactBinaryNotFoundException(artifact.getSha1Hash()));

            final String ifMatch = RequestResponseContextHolder.getHttpServletRequest().getHeader(HttpHeaders.IF_MATCH);
            if (ifMatch != null && !HttpUtil.matchesHttpHeader(ifMatch, artifact.getSha1Hash())) {
                result = new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
            } else {
                final ActionStatus actionStatus = RequestResponseContextHolder.getHttpServletRequest().getHeader("Range") == null
                        ? logDownload(RequestResponseContextHolder.getHttpServletRequest(), target, module.getId())
                        : null; // range request - could have too many - so doesn't check action, don't log action status, and don't publish events
                result = FileStreamingUtil.writeFileResponse(file, artifact.getFilename(), artifact.getCreatedAt(),
                        RequestResponseContextHolder.getHttpServletResponse(),
                        RequestResponseContextHolder.getHttpServletRequest(),
                        (length, shippedSinceLastEvent, total) -> {
                            if (actionStatus != null) {
                                eventPublisher.publishEvent(new DownloadProgressEvent(
                                        tenantAware.getCurrentTenant(), actionStatus.getId(), shippedSinceLastEvent,
                                        serviceMatcher != null ? serviceMatcher.getBusId() : bus.getId()));
                            }
                        });
            }
        }
        return result;
    }

    // Exception squid:S3655 - Optional access is checked in checkModule subroutine
    @SuppressWarnings("squid:S3655")
    @Override
    public ResponseEntity<Void> downloadArtifactMd5(
            final String tenant,
            final String controllerId,
            final Long softwareModuleId,
            final String fileName) {
        final Target target = findTarget(controllerId);

        final SoftwareModule module = controllerManagement.getSoftwareModule(softwareModuleId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, softwareModuleId));

        if (checkModule(fileName, module)) {
            log.warn("Software module with id {} could not be found (2).", softwareModuleId);
            return ResponseEntity.notFound().build();
        }

        final Artifact artifact = module.getArtifactByFilename(fileName)
                .orElseThrow(() -> new EntityNotFoundException(Artifact.class, fileName));

        try {
            logDownload(RequestResponseContextHolder.getHttpServletRequest(), target, module.getId());
            writeMD5FileResponse(RequestResponseContextHolder.getHttpServletResponse(), artifact.getMd5Hash(), fileName);
        } catch (final IOException e) {
            log.error("Failed to stream MD5 File", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<DdiDeploymentBase> getControllerDeploymentBaseAction(
            final String tenant,
            final String controllerId,
            final Long actionId,
            final int resource,
            final Integer actionHistoryMessageCount) {
        log.debug("getControllerDeploymentBaseAction({},{})", controllerId, resource);

        final Target target = findTarget(controllerId);
        final Action action = findActionForTarget(actionId, target);

        checkAndCancelExpiredAction(action);

        if (!action.isCancelingOrCanceled() && !action.isWaitingConfirmation()) {
            final DdiDeploymentBase base = generateDdiDeploymentBase(target, action, actionHistoryMessageCount);
            log.debug("Found an active UpdateAction for target {}. returning deployment: {}", controllerId, base);

            controllerManagement.registerRetrieved(action.getId(), RepositoryConstants.SERVER_MESSAGE_PREFIX +
                    "Target retrieved update action and should start now the download.");

            return new ResponseEntity<>(base, HttpStatus.OK);
        }

        return ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<Void> postDeploymentBaseActionFeedback(
            final DdiActionFeedback feedback,
            final String tenant,
            final String controllerId,
            final Long actionId) {
        log.debug("postDeploymentBaseActionFeedback for target [{},{}]: {}", controllerId, actionId, feedback);

        final Target target = findTarget(controllerId);
        final Action action = findActionForTarget(actionId, target);

        if (action.isWaitingConfirmation()) {
            return ResponseEntity.notFound().build();
        }

        if (!action.isActive()) {
            log.warn("Updating action {} with feedback {} not possible since action not active anymore.", action.getId(), feedback.getStatus());
            return new ResponseEntity<>(HttpStatus.GONE);
        }

        controllerManagement.addUpdateActionStatus(generateUpdateStatus(feedback, controllerId, actionId));

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> putConfigData(
            final DdiConfigData configData,
            final String tenant,
            final String controllerId) {
        controllerManagement.updateControllerAttributes(controllerId, configData.getData(), getUpdateMode(configData));
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<DdiCancel> getControllerCancelAction(
            final String tenant,
            final String controllerId,
            final Long actionId) {
        log.debug("getControllerCancelAction({})", controllerId);

        final Target target = findTarget(controllerId);
        final Action action = findActionForTarget(actionId, target);

        if (action.isCancelingOrCanceled()) {
            final DdiCancel cancel = new DdiCancel(String.valueOf(action.getId()), new DdiCancelActionToStop(String.valueOf(action.getId())));
            log.debug("Found an active CancelAction for target {}. returning cancel: {}", controllerId, cancel);

            controllerManagement.registerRetrieved(action.getId(), RepositoryConstants.SERVER_MESSAGE_PREFIX +
                    "Target retrieved cancel action and should start now the cancellation.");

            return new ResponseEntity<>(cancel, HttpStatus.OK);
        }

        return ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<Void> postCancelActionFeedback(
            final DdiActionFeedback feedback,
            final String tenant,
            final String controllerId,
            final Long actionId) {
        log.debug("provideCancelActionFeedback for target [{}]: {}", controllerId, feedback);

        final Target target = findTarget(controllerId);
        final Action action = findActionForTarget(actionId, target);

        controllerManagement
                .addCancelActionStatus(generateActionCancelStatus(feedback, target, action.getId(), entityFactory));
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<DdiDeploymentBase> getControllerInstalledAction(
            final String tenant,
            final String controllerId,
            final Long actionId,
            final Integer actionHistoryMessageCount) {
        log.debug("getControllerInstalledAction({})", controllerId);

        final Target target = findTarget(controllerId);
        final Action action = findActionForTarget(actionId, target);

        if (action.isActive() || action.isCancelingOrCanceled()) {
            return ResponseEntity.notFound().build();
        }

        final DdiDeploymentBase base = generateDdiDeploymentBase(target, action, actionHistoryMessageCount);

        log.debug("Found an installed UpdateAction for target {}. returning deployment: {}", controllerId, base);
        return new ResponseEntity<>(base, HttpStatus.OK);

    }

    @Override
    public ResponseEntity<DdiConfirmationBase> getConfirmationBase(final String tenant, final String controllerId) {
        log.debug("getConfirmationBase is called [controllerId={}].", controllerId);
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotExist(controllerId, IpUtil
                .getClientIpFromRequest(RequestResponseContextHolder.getHttpServletRequest(), securityProperties));
        final Action activeAction = controllerManagement.findActiveActionWithHighestWeight(controllerId).orElse(null);

        final DdiAutoConfirmationState autoConfirmationState = getAutoConfirmationState(controllerId);

        final DdiConfirmationBase confirmationBase = DataConversionHelper.createConfirmationBase(
                target, activeAction, autoConfirmationState, tenantAware);
        return new ResponseEntity<>(confirmationBase, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DdiConfirmationBaseAction> getConfirmationBaseAction(
            final String tenant,
            final String controllerId,
            final Long actionId,
            final int resource,
            final Integer actionHistoryMessageCount) {
        log.debug("getConfirmationBaseAction({},{})", controllerId, resource);

        final Target target = findTarget(controllerId);
        final Action action = findActionForTarget(actionId, target);

        checkAndCancelExpiredAction(action);

        if (!action.isCancelingOrCanceled() && action.isWaitingConfirmation()) {
            final DdiConfirmationBaseAction base = generateDdiConfirmationBase(target, action, actionHistoryMessageCount);
            log.debug("Found an active UpdateAction for target {}. Returning confirmation: {}", controllerId, base);

            return new ResponseEntity<>(base, HttpStatus.OK);
        }

        return ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<Void> postConfirmationActionFeedback(
            final DdiConfirmationFeedback feedback,
            final String tenant,
            final String controllerId,
            final Long actionId) {
        log.debug("provideConfirmationActionFeedback with feedback [controllerId={}, actionId={}]: {}", controllerId, actionId, feedback);

        final Target target = findTarget(controllerId);
        final Action action = findActionForTarget(actionId, target);
        try {
            switch (feedback.getConfirmation()) {
                case CONFIRMED:
                    log.info("Controller confirmed the action (actionId: {}, controllerId: {}) as we got {} report.",
                            actionId, controllerId, feedback.getConfirmation());
                    confirmationManagement.confirmAction(actionId, feedback.getCode(), feedback.getDetails());
                    break;
                case DENIED:
                default:
                    log.debug("Controller denied the action (actionId: {}, controllerId: {}) as we got {} report.",
                            actionId, controllerId, feedback.getConfirmation());
                    confirmationManagement.denyAction(actionId, feedback.getCode(), feedback.getDetails());
                    break;
            }
        } catch (final InvalidConfirmationFeedbackException e) {
            if (e.getReason() == InvalidConfirmationFeedbackException.Reason.ACTION_CLOSED) {
                log.warn("Updating action {} with confirmation {} not possible since action not active anymore.",
                        action.getId(), feedback.getConfirmation());
                return new ResponseEntity<>(HttpStatus.GONE);
            } else if (e.getReason() == InvalidConfirmationFeedbackException.Reason.NOT_AWAITING_CONFIRMATION) {
                log.debug("Action is not waiting for confirmation, deny request.");
                return ResponseEntity.notFound().build();
            }
        }

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> activateAutoConfirmation(
            final String tenant, final String controllerId, final DdiActivateAutoConfirmation body) {
        final String initiator = body == null ? null : body.getInitiator();
        final String remark = body == null ? FALLBACK_REMARK : body.getRemark();
        log.debug("Activate auto-confirmation request for device '{}' with payload: [initiator='{}' | remark='{}'",
                controllerId, initiator, remark);
        confirmationManagement.activateAutoConfirmation(controllerId, initiator, remark);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deactivateAutoConfirmation(final String tenant, final String controllerId) {
        log.debug("Deactivate auto-confirmation request for device ‘{}‘", controllerId);
        confirmationManagement.deactivateAutoConfirmation(controllerId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> setAssignedOfflineVersion(
            final DdiAssignedVersion ddiAssignedVersion,
            final String tenant,
            final String controllerId) {
        boolean updated = controllerManagement.updateOfflineAssignedVersion(controllerId,
                ddiAssignedVersion.getName(), ddiAssignedVersion.getVersion());
        if (updated) {
            return new ResponseEntity<>(HttpStatus.CREATED);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private static boolean checkModule(final String fileName, final SoftwareModule module) {
        return module == null || module.getArtifactByFilename(fileName).isEmpty();
    }

    private static HandlingType calculateDownloadType(final Action action) {
        if (action.isDownloadOnly() || action.isForcedOrTimeForced()) {
            return HandlingType.FORCED;
        }
        return HandlingType.ATTEMPT;
    }

    private static DdiMaintenanceWindowStatus calculateMaintenanceWindow(final Action action) {
        if (action.hasMaintenanceSchedule()) {
            return action.isMaintenanceWindowAvailable() ? DdiMaintenanceWindowStatus.AVAILABLE
                    : DdiMaintenanceWindowStatus.UNAVAILABLE;
        }
        return null;
    }

    private static HandlingType calculateUpdateType(final Action action, final HandlingType downloadType) {
        if (action.isDownloadOnly()) {
            return HandlingType.SKIP;
        } else if (action.hasMaintenanceSchedule()) {
            return action.isMaintenanceWindowAvailable() ? downloadType : HandlingType.SKIP;
        }
        return downloadType;
    }

    private static void addMessageIfEmpty(final String text, final List<String> messages) {
        if (messages != null && messages.isEmpty()) {
            messages.add(RepositoryConstants.SERVER_MESSAGE_PREFIX + text + ".");
        }
    }

    private static ActionStatusCreate generateActionCancelStatus(
            final DdiActionFeedback feedback, final Target target, final Long actionId, final EntityFactory entityFactory) {
        final ActionStatusCreate actionStatusCreate = entityFactory.actionStatus().create(actionId).occurredAt(feedback.getTimestamp());
        final List<String> messages = new ArrayList<>();
        final Status status;
        switch (feedback.getStatus().getExecution()) {
            case CANCELED: {
                status = handleCaseCancelCanceled(feedback, target, actionId, messages);
                break;
            }
            case REJECTED: {
                log.info("Target rejected the cancellation request (actionId: {}, controllerId: {}).", actionId,
                        target.getControllerId());
                status = Status.CANCEL_REJECTED;
                messages.add(RepositoryConstants.SERVER_MESSAGE_PREFIX + "Target rejected the cancellation request.");
                break;
            }
            case CLOSED: {
                status = handleCancelClosedCase(feedback, messages);
                break;
            }
            default: {
                status = Status.RUNNING;
                break;
            }
        }

        if (feedback.getStatus().getDetails() != null) {
            messages.addAll(feedback.getStatus().getDetails());
        }

        final Integer code = feedback.getStatus().getCode();
        if (code != null) {
            actionStatusCreate.code(code);
            messages.add("Device reported status code: " + code);
        }

        return actionStatusCreate.status(status).messages(messages);
    }

    private static Status handleCancelClosedCase(final DdiActionFeedback feedback, final List<String> messages) {
        final Status status;
        if (feedback.getStatus().getResult().getFinished() == FinalResult.FAILURE) {
            status = Status.ERROR;
            addMessageIfEmpty("Target was not able to complete cancellation", messages);
        } else {
            status = Status.CANCELED;
            addMessageIfEmpty("Cancellation confirmed", messages);
        }
        return status;
    }

    private static Status handleCaseCancelCanceled(
            final DdiActionFeedback feedback, final Target target, final Long actionId, final List<String> messages) {
        final Status status;
        log.error(
                "Target reported cancel for a cancel which is not supported by the server (actionId: {}, controllerId: {}) as we got {} report.",
                actionId, target.getControllerId(), feedback.getStatus().getExecution());
        status = Status.WARNING;
        messages.add(RepositoryConstants.SERVER_MESSAGE_PREFIX +
                "Target reported cancel for a cancel which is not supported by the server.");
        return status;
    }

    /**
     * Retrieve the update mode from the given update message.
     */
    private static UpdateMode getUpdateMode(final DdiConfigData configData) {
        final DdiUpdateMode mode = configData.getMode();
        if (mode != null) {
            return UpdateMode.valueOf(mode.name());
        }
        return null;
    }

    /**
     * Write a md5 file response.
     *
     * @param response the response
     * @param md5Hash of the artifact
     * @param filename as provided by the client
     * @throws IOException cannot write output stream
     */
    private static void writeMD5FileResponse(final HttpServletResponse response, final String md5Hash, final String filename)
            throws IOException {
        if (md5Hash == null) {
            throw new IllegalArgumentException("MD5 hash must not be null");
        }

        final byte[] content = (md5Hash + "  " + filename).getBytes(StandardCharsets.US_ASCII);
        response.setContentType("text/plain");
        response.setContentLength(content.length);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + filename + ARTIFACT_MD5_DWNL_SUFFIX);
        response.getOutputStream().write(content);
    }

    private ActionStatus logDownload(final HttpServletRequest request, final Target target, final Long module) {
        final Action action = controllerManagement
                .getActionForDownloadByTargetAndSoftwareModule(target.getControllerId(), module)
                .orElseThrow(() -> new SoftwareModuleNotAssignedToTargetException(module, target.getControllerId()));
        return controllerManagement.addInformationalActionStatus(
                entityFactory.actionStatus()
                        .create(action.getId())
                        .status(Status.DOWNLOAD)
                        .message(RepositoryConstants.SERVER_MESSAGE_PREFIX + "Target downloads " + request.getRequestURI()));
    }

    private ActionStatusCreate generateUpdateStatus(final DdiActionFeedback feedback, final String controllerId, final Long actionId) {
        final ActionStatusCreate actionStatusCreate = entityFactory.actionStatus().create(actionId).occurredAt(feedback.getTimestamp());
        final List<String> messages = new ArrayList<>();

        if (!CollectionUtils.isEmpty(feedback.getStatus().getDetails())) {
            messages.addAll(feedback.getStatus().getDetails());
        }

        final Integer code = feedback.getStatus().getCode();
        if (code != null) {
            actionStatusCreate.code(code);
            messages.add("Device reported status code: " + code);
        }

        final Status status;
        switch (feedback.getStatus().getExecution()) {
            case CANCELED: {
                log.debug("Controller confirmed cancel (actionId: {}, controllerId: {}) as we got {} report.", actionId,
                        controllerId, feedback.getStatus().getExecution());
                status = Status.CANCELED;
                addMessageIfEmpty("Target confirmed cancellation.", messages);
                break;
            }
            case REJECTED: {
                log.info("Controller reported internal error (actionId: {}, controllerId: {}) as we got {} report.",
                        actionId, controllerId, feedback.getStatus().getExecution());
                status = Status.WARNING;
                addMessageIfEmpty("Target REJECTED update", messages);
                break;
            }
            case CLOSED: {
                status = handleClosedCase(feedback, controllerId, actionId, messages);
                break;
            }
            case DOWNLOAD: {
                log.debug("Controller confirmed status of download (actionId: {}, controllerId: {}) as we got {} report.",
                        actionId, controllerId, feedback.getStatus().getExecution());
                status = Status.DOWNLOAD;
                addMessageIfEmpty("Target confirmed download start", messages);
                break;
            }
            case DOWNLOADED: {
                log.debug("Controller confirmed download (actionId: {}, controllerId: {}) as we got {} report.", actionId,
                        controllerId, feedback.getStatus().getExecution());
                status = Status.DOWNLOADED;
                addMessageIfEmpty("Target confirmed download finished", messages);
                break;
            }
            default: {
                status = handleDefaultCase(feedback, controllerId, actionId, messages);
                break;
            }
        }

        return actionStatusCreate.status(status).messages(messages);
    }

    private Status handleDefaultCase(
            final DdiActionFeedback feedback, final String controllerId, final Long actionId, final List<String> messages) {
        final Status status;
        log.debug("Controller reported intermediate status (actionId: {}, controllerId: {}) as we got {} report.",
                actionId, controllerId, feedback.getStatus().getExecution());
        status = Status.RUNNING;
        addMessageIfEmpty("Target reported " + feedback.getStatus().getExecution(), messages);
        return status;
    }

    private Status handleClosedCase(
            final DdiActionFeedback feedback, final String controllerId, final Long actionId, final List<String> messages) {
        final Status status;
        log.debug("Controller reported closed (actionId: {}, controllerId: {}) as we got {} report.", actionId,
                controllerId, feedback.getStatus().getExecution());
        if (feedback.getStatus().getResult().getFinished() == FinalResult.FAILURE) {
            status = Status.ERROR;
            addMessageIfEmpty("Target reported CLOSED with ERROR!", messages);
        } else {
            status = Status.FINISHED;
            addMessageIfEmpty("Target reported CLOSED with OK!", messages);
        }
        return status;
    }

    private Target findTarget(final String controllerId) {
        return controllerManagement.getByControllerId(controllerId)
                .orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));
    }

    private Action findActionForTarget(final Long actionId, final Target target) {
        final Action action = controllerManagement.findActionWithDetails(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));
        return verifyActionBelongsToTarget(action, target);
    }

    private Action verifyActionBelongsToTarget(final Action action, final Target target) {
        if (!action.getTarget().getId().equals(target.getId())) {
            log.debug(GIVEN_ACTION_IS_NOT_ASSIGNED_TO_GIVEN_TARGET, action.getId(), target.getId());
            throw new EntityNotFoundException(
                    "Not a valid action (" + action.getId() + ") for target: " + target.getControllerId(), null);
        }
        return action;
    }

    /**
     * If the action has a maintenance schedule defined but is no longer valid, cancel the action.
     *
     * @param action is the {@link Action} to check.
     */
    private void checkAndCancelExpiredAction(final Action action) {
        if (action != null && action.hasMaintenanceSchedule() && action.isMaintenanceScheduleLapsed()) {
            try {
                controllerManagement.cancelAction(action.getId());
            } catch (final CancelActionNotAllowedException e) {
                log.info("Cancel action not allowed: {}", e.getMessage());
            }
        }
    }

    private DdiDeploymentBase generateDdiDeploymentBase(final Target target, final Action action, final Integer actionHistoryMessageCount) {
        final DdiActionHistory actionHistory = generateDdiActionHistory(action, actionHistoryMessageCount).orElse(null);
        final DdiDeployment ddiDeployment = generateDdiDeployment(target, action);
        return new DdiDeploymentBase(Long.toString(action.getId()), ddiDeployment, actionHistory);
    }

    private DdiConfirmationBaseAction generateDdiConfirmationBase(
            final Target target, final Action action, final Integer actionHistoryMessageCount) {
        final DdiActionHistory actionHistory = generateDdiActionHistory(action, actionHistoryMessageCount).orElse(null);
        final DdiDeployment ddiDeployment = generateDdiDeployment(target, action);
        return new DdiConfirmationBaseAction(Long.toString(action.getId()), ddiDeployment, actionHistory);
    }

    private DdiDeployment generateDdiDeployment(final Target target, final Action action) {
        final List<DdiChunk> chunks = DataConversionHelper.createChunks(
                target, action, artifactUrlHandler,
                systemManagement, new ServletServerHttpRequest(RequestResponseContextHolder.getHttpServletRequest()),
                controllerManagement);
        final HandlingType downloadType = calculateDownloadType(action);
        final HandlingType updateType = calculateUpdateType(action, downloadType);
        final DdiMaintenanceWindowStatus maintenanceWindow = calculateMaintenanceWindow(action);
        return new DdiDeployment(downloadType, updateType, chunks, maintenanceWindow);
    }

    private Optional<DdiActionHistory> generateDdiActionHistory(final Action action, final Integer actionHistoryMessageCount) {
        final List<String> actionHistoryMsgs = controllerManagement.getActionHistoryMessages(action.getId(),
                actionHistoryMessageCount == null
                        ? Integer.parseInt(DdiRestConstants.NO_ACTION_HISTORY)
                        : actionHistoryMessageCount);
        return actionHistoryMsgs.isEmpty()
                ? Optional.empty()
                : Optional.of(new DdiActionHistory(action.getStatus().name(), actionHistoryMsgs));
    }

    private DdiAutoConfirmationState getAutoConfirmationState(final String controllerId) {
        return confirmationManagement.getStatus(controllerId).map(status -> {
            final DdiAutoConfirmationState state = DdiAutoConfirmationState.active(status.getActivatedAt());
            state.setInitiator(status.getInitiator());
            state.setRemark(status.getRemark());
            log.trace("Returning state auto-conf state active [initiator='{}' | activatedAt={}] for device {}",
                    controllerId, status.getInitiator(), status.getActivatedAt());
            return state;
        }).orElseGet(() -> {
            log.trace("Returning state auto-conf state disabled for device {}", controllerId);
            return DdiAutoConfirmationState.disabled();
        });
    }
}