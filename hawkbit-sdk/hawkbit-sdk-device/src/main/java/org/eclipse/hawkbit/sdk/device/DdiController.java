/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk.device;

import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.ddi.json.model.DdiActionFeedback;
import org.eclipse.hawkbit.ddi.json.model.DdiChunk;
import org.eclipse.hawkbit.ddi.json.model.DdiConfigData;
import org.eclipse.hawkbit.ddi.json.model.DdiConfirmationFeedback;
import org.eclipse.hawkbit.ddi.json.model.DdiControllerBase;
import org.eclipse.hawkbit.ddi.json.model.DdiDeployment;
import org.eclipse.hawkbit.ddi.json.model.DdiDeploymentBase;
import org.eclipse.hawkbit.ddi.json.model.DdiProgress;
import org.eclipse.hawkbit.ddi.json.model.DdiResult;
import org.eclipse.hawkbit.ddi.json.model.DdiStatus;
import org.eclipse.hawkbit.ddi.json.model.DdiUpdateMode;
import org.eclipse.hawkbit.ddi.rest.api.DdiRootControllerRestApi;
import org.eclipse.hawkbit.sdk.Certificate;
import org.eclipse.hawkbit.sdk.Controller;
import org.eclipse.hawkbit.sdk.HawkbitClient;
import org.eclipse.hawkbit.sdk.Tenant;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Class representing DDI device connecting directly to hawkBit.
 */
@Slf4j
@Getter
public class DdiController {

    private static final String LOG_PREFIX = "[{}:{}] ";

    // TODO - make them configurable
    private static final long IMMEDIATE_MS = 10;
    private static final long DEFAULT_POLL_MS = 5_000;

    private static final String DEPLOYMENT_BASE_LINK = "deploymentBase";
    private static final String CONFIRMATION_BASE_LINK = "confirmationBase";
    private static final String CANCEL_ACTION_LINK = "cancelAction";

    private final Tenant tenant;
    private final Controller controller;
    private final UpdateHandler updateHandler;
    private final DdiRootControllerRestApi ddiApi;

    // configuration
    private final String targetSecurityToken;
    private final Certificate certificate;

    @Setter
    @Accessors(chain = true)
    private long overridePollMillis = -1; // -1 means disabled

    // state
    @SuppressWarnings("java:S3077") // volatile used only for the reference as expected
    private volatile ScheduledExecutorService executorService;
    private volatile Long currentActionId;

    private volatile Long lastActionId;

    /**
     * Creates a new device instance.
     *
     * @param tenant the tenant of the device belongs to
     * @param controller the controller
     * @param hawkbitClient a factory for creating to {@link DdiRootControllerRestApi} (and used) for communication to hawkBit
     */
    public DdiController(final Tenant tenant, final Controller controller, final UpdateHandler updateHandler, final HawkbitClient hawkbitClient) {
        this.tenant = tenant;
        this.controller = controller;
        this.targetSecurityToken = controller.getSecurityToken();
        this.certificate = controller.getCertificate();
        this.updateHandler = updateHandler == null ? UpdateHandler.SKIP : updateHandler;
        ddiApi = hawkbitClient.ddiService(DdiRootControllerRestApi.class, tenant, controller);
    }

    public String getTenantId() {
        return tenant.getTenantId();
    }
    
    public String getControllerId() {
        return controller.getControllerId();
    }

    // expects single threaded {@link java.util.concurrent.ScheduledExecutorService}
    public void start(final ScheduledExecutorService executorService) {
        stop();

        Objects.requireNonNull(executorService, "Require non null executor!");
        this.executorService = executorService;
        executorService.submit(this::poll);
    }

    public void stop() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        executorService = null;
        lastActionId = null;
        currentActionId = null;
    }

    public void updateAttribute(final String mode, final String key, final String value) {
        final DdiUpdateMode updateMode = switch (mode.toLowerCase()) {
            case "replace" -> DdiUpdateMode.REPLACE;
            case "remove" -> DdiUpdateMode.REMOVE;
            default -> DdiUpdateMode.MERGE;
        };

        final DdiConfigData configData = new DdiConfigData(Collections.singletonMap(key, value), updateMode);

        getDdiApi().putConfigData(configData, getTenantId(), getControllerId());
    }

    public void sendFeedback(final UpdateStatus updateStatus) {
        log.debug(LOG_PREFIX + "Send feedback {} -> {}", getTenantId(), getControllerId(), currentActionId, updateStatus);
        try {
            getDdiApi().postDeploymentBaseActionFeedback(updateStatus.feedback(), getTenantId(), getControllerId(),
                    currentActionId);
        } catch (final RuntimeException e) {
            log.error(LOG_PREFIX + "Failed to send feedback {} -> {}", getTenantId(), getControllerId(),
                    currentActionId, updateStatus, e);
        }

        if (updateStatus.status() == UpdateStatus.Status.SUCCESSFUL ||
                updateStatus.status() == UpdateStatus.Status.FAILURE) {
            lastActionId = currentActionId;
            currentActionId = null;
        }
    }

    public void sendCancelFeedback(long actionId) {
        try {
            log.info(LOG_PREFIX + "Sending cancelation feedback for action with id : {}", getTenantId(), getControllerId(), actionId);
            getDdiApi().postCancelActionFeedback(new DdiActionFeedback(
                    new DdiStatus(DdiStatus.ExecutionStatus.CLOSED, new DdiResult(DdiResult.FinalResult.SUCCESS,
                            new DdiProgress(100, 100)), 200, List.of("Successfully cancelled by the controller."))
            ), getTenantId(), getControllerId(), actionId);
        } catch (Exception ex) {
            log.error(LOG_PREFIX + "Failed to send cancelation feedback {}", getTenantId(), getControllerId(),
                    actionId, ex);
        }
    }

    private void poll() {
        log.debug(LOG_PREFIX + " Polling ...", getTenantId(), getControllerId());
        Optional.ofNullable(executorService).ifPresent(executor ->
                getControllerBase().ifPresentOrElse(
                        controllerBase -> {
                            final Optional<Link> confirmationBaseLink = getRequiredLink(controllerBase, CONFIRMATION_BASE_LINK);
                            if (confirmationBaseLink.isPresent()) {
                                final long actionId = getActionId(confirmationBaseLink.get());
                                log.info(LOG_PREFIX + "Confirmation is required for action {}!", getTenantId(),
                                        getControllerId(), actionId);
                                // TODO - confirmation handler
                                sendConfirmationFeedback(actionId);
                                executor.schedule(this::poll, IMMEDIATE_MS, TimeUnit.MILLISECONDS);
                            } else {
                                getRequiredLink(controllerBase, DEPLOYMENT_BASE_LINK).flatMap(this::getActionWithDeployment)
                                        .ifPresentOrElse(actionWithDeployment -> {
                                            final long actionId = actionWithDeployment.getKey();
                                            if (currentActionId == null) {
                                                if (lastActionId != null && lastActionId == actionId) {
                                                    log.info(LOG_PREFIX + "Still receive the last action {}",
                                                            getTenantId(), getControllerId(), actionId);
                                                    return;
                                                }

                                                log.info(LOG_PREFIX + "Process action {}", getTenantId(), getControllerId(),
                                                        actionId);
                                                final DdiDeployment deployment = actionWithDeployment.getValue().getDeployment();
                                                final DdiDeployment.HandlingType updateType = deployment.getUpdate();
                                                final List<DdiChunk> modules = deployment.getChunks();

                                                currentActionId = actionId;
                                                executor.submit(updateHandler.getUpdateProcessor(this, updateType, modules));
                                            } else if (currentActionId != actionId) {
                                                // currentActionId had failed to be processed and new one had been initiated
                                                // try cancel current and process new one
                                                log.info(LOG_PREFIX + "Action {} is canceled while in process (new {})!", getTenantId(),
                                                        getControllerId(), currentActionId, actionId);
                                                cancelActionByCancellationLink(controllerBase, currentActionId);
                                                currentActionId = actionId;
                                                // then process the new one
                                                poll();
                                            } // else same action - already processing
                                        }, () -> {
                                            cancelActionByCancellationLink(controllerBase, -1);
                                            // reset current action id - on next poll should be available deploymentBase
                                            currentActionId = null;
                                        });
                                executor.schedule(this::poll, getPollMillis(controllerBase), TimeUnit.MILLISECONDS);
                            }
                        },
                        () -> // error has occurred or no controller base hasn't been acquired
                                executor.schedule(this::poll, DEFAULT_POLL_MS, TimeUnit.MILLISECONDS)));
    }

    private Optional<DdiControllerBase> getControllerBase() {
        log.trace(LOG_PREFIX + "Polling ...", getTenantId(), getControllerId());
        final ResponseEntity<DdiControllerBase> poll;
        try {
            poll = getDdiApi().getControllerBase(getTenantId(), getControllerId());
        } catch (final RuntimeException ex) {
            log.error(LOG_PREFIX + "Failed base poll", getTenantId(), getControllerId(), ex);
            return Optional.empty();
        }

        if (poll.getStatusCode() != HttpStatus.OK) {
            log.error(LOG_PREFIX + "Failed base poll {}", getTenantId(), getControllerId(), poll.getStatusCode());
            return Optional.empty();
        }

        return Optional.ofNullable(poll.getBody());
    }

    private void cancelActionByCancellationLink(DdiControllerBase controllerBase, long actionToBeCanceled) {
        getRequiredLink(controllerBase, CANCEL_ACTION_LINK).ifPresentOrElse(link -> {
                final long actionId = actionToBeCanceled == -1 ? getActionIdFromCancellationLink(link) : actionToBeCanceled;
                log.info(LOG_PREFIX + "Cancelling current action {}", getTenantId(), getControllerId(), actionId);
                sendCancelFeedback(actionId);
            }, () -> log.info(LOG_PREFIX + "Action {} is canceled while in process (not returned)!", getTenantId(), getControllerId(), getCurrentActionId())
        );
    }

    private Optional<Link> getRequiredLink(final DdiControllerBase controllerBase, final String nameOfTheLink) {
        final Optional<Link> link = controllerBase != null ? controllerBase.getLink(nameOfTheLink) : Optional.empty();
        link.ifPresentOrElse(
                l -> log.debug(LOG_PREFIX + "Polling finished. Has {} link: {}", getTenantId(), getControllerId(), nameOfTheLink, l),
                () -> log.trace(LOG_PREFIX + "Polling finished. No {} link", getTenantId(), getControllerId(), nameOfTheLink));
        return link;
    }

    private long getPollMillis(final DdiControllerBase controllerBase) {
        if (overridePollMillis >= 0) {
            return overridePollMillis;
        }

        final String pollingTimeFromResponse = controllerBase.getConfig().getPolling().getSleep();
        if (pollingTimeFromResponse == null) {
            return DEFAULT_POLL_MS;
        } else {
            final LocalTime localtime = LocalTime.parse(pollingTimeFromResponse);
            return localtime.getLong(ChronoField.MILLI_OF_DAY);
        }
    }

    private Optional<Map.Entry<Long, DdiDeploymentBase>> getActionWithDeployment(final Link deploymentBaseLink) {
        final long actionId = getActionId(deploymentBaseLink);
        final ResponseEntity<DdiDeploymentBase> action = getDdiApi()
                .getControllerDeploymentBaseAction(getTenantId(), getControllerId(), actionId, -1, null);
        if (action.getStatusCode() != HttpStatus.OK) {
            log.warn(LOG_PREFIX + "Fail to get deployment action: {} -> {}", getTenantId(), getControllerId(), actionId,
                    action.getStatusCode());
            return Optional.empty();
        }

        return Optional.ofNullable(action.getBody() == null ? null : new AbstractMap.SimpleEntry<>(actionId, action.getBody()));
    }

    private void sendConfirmationFeedback(final long actionId) {
        final DdiConfirmationFeedback ddiConfirmationFeedback = new DdiConfirmationFeedback(
                DdiConfirmationFeedback.Confirmation.CONFIRMED, 0, Collections.singletonList(
                "the confirmation status for the device is" + DdiConfirmationFeedback.Confirmation.CONFIRMED));
        getDdiApi().postConfirmationActionFeedback(ddiConfirmationFeedback, getTenantId(), getControllerId(), actionId);
    }

    private long getActionId(final Link link) {
        final String href = link.getHref();
        return Long.parseLong(href.substring(href.lastIndexOf('/') + 1, href.indexOf('?')));
    }

    private long getActionIdFromCancellationLink(final Link link) {
        final String href = link.getHref();
        final String[] split = href.split("/");
        return Long.parseLong(split[split.length - 1]);
    }
}