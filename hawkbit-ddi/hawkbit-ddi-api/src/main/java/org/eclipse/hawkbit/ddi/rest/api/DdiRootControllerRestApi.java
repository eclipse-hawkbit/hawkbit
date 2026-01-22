/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.rest.api;

import static org.eclipse.hawkbit.rest.ApiResponsesConstants.GONE_410;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.METHOD_NOT_ALLOWED_405;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.InputStream;
import java.lang.annotation.Target;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.hawkbit.ddi.json.model.DdiActionFeedback;
import org.eclipse.hawkbit.ddi.json.model.DdiActivateAutoConfirmation;
import org.eclipse.hawkbit.ddi.json.model.DdiArtifact;
import org.eclipse.hawkbit.ddi.json.model.DdiAssignedVersion;
import org.eclipse.hawkbit.ddi.json.model.DdiAutoConfirmationState;
import org.eclipse.hawkbit.ddi.json.model.DdiCancel;
import org.eclipse.hawkbit.ddi.json.model.DdiConfigData;
import org.eclipse.hawkbit.ddi.json.model.DdiConfirmationBase;
import org.eclipse.hawkbit.ddi.json.model.DdiConfirmationBaseAction;
import org.eclipse.hawkbit.ddi.json.model.DdiConfirmationFeedback;
import org.eclipse.hawkbit.ddi.json.model.DdiControllerBase;
import org.eclipse.hawkbit.ddi.json.model.DdiDeploymentBase;
import org.eclipse.hawkbit.rest.ApiResponsesConstants.GetIfExistResponses;
import org.eclipse.hawkbit.rest.ApiResponsesConstants.GetResponses;
import org.eclipse.hawkbit.rest.ApiResponsesConstants.PostUpdateResponses;
import org.eclipse.hawkbit.rest.ApiResponsesConstants.PutResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST resource handling for root controller CRUD operations.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Tag(name = "DDI Root Controller", description = "REST resource handling for root controller CRUD operations")
public interface DdiRootControllerRestApi {

    /**
     * The base URL mapping of the direct device integration rest resources.
     */
    String CONTROLLER_V1 = "/{tenant}/controller/v1";

    /**
     * Deployment action resources.
     */
    String DEPLOYMENT_BASE = "deploymentBase";
    /**
     * Confirmation base resource.
     */
    String CONFIRMATION_BASE = "confirmationBase";
    /**
     * Installed action resources.
     */
    String INSTALLED_BASE = "installedBase";
    /**
     * Feedback channel.
     */
    String FEEDBACK = "feedback";
    /**
     * Cancel action resources.
     */
    String CANCEL_ACTION = "cancelAction";
    /**
     * Config data action resources.
     */
    String CONFIG_DATA = "configData";
    /**
     * Activate auto-confirm
     */
    String ACTIVATE_AUTO_CONFIRM = "activateAutoConfirm";
    /**
     * Deactivate auto-confirm
     */
    String DEACTIVATE_AUTO_CONFIRM = "deactivateAutoConfirm";
    /**
     * Media type for CBOR content.
     */
    String MEDIA_TYPE_APPLICATION_CBOR = "application/cbor";
    /**
     * File suffix for MDH hash download (see Linux md5sum).
     */
    String ARTIFACT_MD5_DOWNLOAD_SUFFIX = ".MD5SUM";
    /**
     * Default value specifying that no action history to be sent as part of response to deploymentBase
     * {@link DdiRootControllerRestApi#getControllerDeploymentBaseAction}, {@link DdiRootControllerRestApi#getConfirmationBaseAction}.
     */
    String NO_ACTION_HISTORY = "0";

    /**
     * Returns all artifacts of a given software module and target.
     *
     * @param tenant of the client
     * @param controllerId of the target that matches to controller id
     * @param softwareModuleId of the software module
     * @return the response
     */
    @Operation(summary = "Return all artifacts of a given software module and target",
            description = "Returns all artifacts that are assigned to the software module")
    @GetIfExistResponses
    @GetMapping(value = CONTROLLER_V1 + "/{controllerId}/softwaremodules/{softwareModuleId}/artifacts",
            produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE, MEDIA_TYPE_APPLICATION_CBOR })
    ResponseEntity<List<DdiArtifact>> getSoftwareModulesArtifacts(
            @PathVariable("tenant") String tenant,
            @PathVariable("controllerId") String controllerId,
            @PathVariable("softwareModuleId") Long softwareModuleId);

    /**
     * Root resource for an individual {@link Target}.
     *
     * @param tenant of the request
     * @param controllerId of the target that matches to controller id
     * @return the response
     */
    @Operation(summary = "Root resource for an individual Target", description = """
            This base resource can be regularly polled by the controller on the provisioning target or device in order to
            retrieve actions that need to be executed. Those are provided as a list of links to give more detailed
            information about the action. Links are only available for initial configuration, open actions, or the latest
            installed action, respectively. The resource supports Etag based modification checks in order to save traffic.
            
            Note: deployments have to be confirmed in order to move on to the next action. Cancellations have to be
            confirmed or rejected.""")
    @GetResponses
    @GetMapping(value = CONTROLLER_V1 + "/{controllerId}",
            produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE, MEDIA_TYPE_APPLICATION_CBOR })
    ResponseEntity<DdiControllerBase> getControllerBase(
            @PathVariable("tenant") String tenant,
            @PathVariable("controllerId") String controllerId);

    /**
     * Handles GET {@link DdiArtifact} download request. This could be full or
     * partial (as specified by RFC7233 (Range Requests)) download request.
     *
     * @param tenant of the request
     * @param controllerId of the target
     * @param softwareModuleId of the parent software module
     * @param fileName of the related local artifact
     * @return response of the servlet which in case of success is status code
     *         {@link HttpStatus#OK} or in case of partial download {@link HttpStatus#PARTIAL_CONTENT}.
     */
    @Operation(summary = "Artifact download",
            description = "Handles GET DdiArtifact download request. This could be full or partial (as specified by RFC7233 (Range Requests)) " +
                    "download request.")
    @GetResponses
    @ApiResponses(value = {
            @ApiResponse(responseCode = INTERNAL_SERVER_ERROR_500, description = "Artifact download or decryption failed",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = CONTROLLER_V1 + "/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/{fileName}")
    ResponseEntity<InputStream> downloadArtifact(
            @PathVariable("tenant") String tenant,
            @PathVariable("controllerId") String controllerId,
            @PathVariable("softwareModuleId") Long softwareModuleId,
            @PathVariable("fileName") String fileName);

    /**
     * Handles GET {@link DdiArtifact} MD5 checksum file download request.
     *
     * @param tenant of the request
     * @param controllerId of the target
     * @param softwareModuleId of the parent software module
     * @param fileName of the related local artifact
     * @return {@link ResponseEntity} with status {@link HttpStatus#OK} if successful
     */
    @Operation(summary = "MD5 checksum download",
            description = "Handles GET {@link DdiArtifact} MD5 checksum file download request.")
    @GetResponses
    @GetMapping(value = CONTROLLER_V1 + "/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/{fileName}" + ARTIFACT_MD5_DOWNLOAD_SUFFIX,
            produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<Void> downloadArtifactMd5(
            @PathVariable("tenant") String tenant,
            @PathVariable("controllerId") String controllerId,
            @PathVariable("softwareModuleId") Long softwareModuleId,
            @PathVariable("fileName") String fileName);

    /**
     * Resource for software module.
     *
     * @param tenant of the request
     * @param controllerId of the target
     * @param actionId of the {@link DdiDeploymentBase} that matches to active actions.
     * @param resource a hashcode of the resource which indicates if the action has been changed, e.g. from 'soft' to 'force' and
     *         the eTag needs to be re-generated
     * @param actionHistoryMessageCount specifies the number of messages to be returned from action history. Regardless of the passed value,
     *         in order to restrict resource utilization by controllers, maximum number of
     *         messages that are retrieved from database is limited by RepositoryConstants#MAX_ACTION_HISTORY_MSG_COUNT.
     *         actionHistoryMessageCount less than zero: retrieves the maximum allowed number of action status messages from history;
     *         actionHistoryMessageCount equal to zero: does not retrieve any message;
     *         actionHistoryMessageCount greater than zero: retrieves the specified number of messages, limited by maximum allowed number.
     * @return the response
     */
    @Operation(summary = "Resource for software module (Deployment Base)", description = """
            Core resource for deployment operations. Contains all information necessary in order to execute the operation.
            
            Keep in mind that the provided download links for the artifacts are generated dynamically by the update server.
            Host, port and path and not guaranteed to be similar to the provided examples below but will be defined at
            runtime.
            """)
    @GetResponses
    @GetMapping(value = CONTROLLER_V1 + "/{controllerId}/" + DEPLOYMENT_BASE + "/{actionId}",
            produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE, MEDIA_TYPE_APPLICATION_CBOR })
    ResponseEntity<DdiDeploymentBase> getControllerDeploymentBaseAction(
            @PathVariable("tenant") String tenant,
            @PathVariable("controllerId") @NotEmpty String controllerId,
            @PathVariable("actionId") @NotNull Long actionId,
            @RequestParam(value = "c", required = false, defaultValue = "-1") int resource,
            @RequestParam(value = "actionHistory", defaultValue = NO_ACTION_HISTORY)
            @Schema(description = """
                    (Optional) GET parameter to retrieve a given number of messages which are previously provided by the
                    device. Useful if the devices sent state information to the feedback channel and never stored them
                    locally.""") Integer actionHistoryMessageCount);

    /**
     * This is the feedback channel for the {@link DdiDeploymentBase} action.
     *
     * @param tenant of the client
     * @param feedback to provide
     * @param controllerId of the target that matches to controller id
     * @param actionId of the action we have feedback for
     * @return the response
     */
    @Operation(summary = "Feedback channel for the DeploymentBase action", description = """
            Feedback channel. It is up to the device how much intermediate feedback is provided.
            However, the action will be kept open until the controller on the device reports a finished (either successful
            or error).
            """)
    @PostUpdateResponses
    @ApiResponses(value = {
            @ApiResponse(responseCode = GONE_410, description = "Action is not active anymore.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = CONTROLLER_V1 + "/{controllerId}/" + DEPLOYMENT_BASE +
            "/{actionId}/" + FEEDBACK, consumes = { APPLICATION_JSON_VALUE, MEDIA_TYPE_APPLICATION_CBOR })
    ResponseEntity<Void> postDeploymentBaseActionFeedback(
            @Valid @RequestBody DdiActionFeedback feedback,
            @PathVariable("tenant") String tenant,
            @PathVariable("controllerId") String controllerId,
            @PathVariable("actionId") @NotNull Long actionId);

    /**
     * This is the feedback channel for the config data action.
     *
     * @param tenant of the client
     * @param configData as body
     * @param controllerId to provide data for
     * @return status of the request
     */
    @Operation(summary = "Feedback channel for the config data action", description = """
            The usual behaviour is that when a new device registers at the server it is requested to provide the meta
            information that will allow the server to identify the device on a hardware level (e.g. hardware revision,
            mac address, serial number etc.).""")
    @PutResponses
    @PutMapping(value = CONTROLLER_V1 + "/{controllerId}/" + CONFIG_DATA,
            consumes = { APPLICATION_JSON_VALUE, MEDIA_TYPE_APPLICATION_CBOR })
    ResponseEntity<Void> putConfigData(
            @Valid @RequestBody DdiConfigData configData,
            @PathVariable("tenant") String tenant,
            @PathVariable("controllerId") String controllerId);

    /**
     * RequestMethod.GET method for the {@link DdiCancel} action.
     *
     * @param tenant of the request
     * @param controllerId ID of the calling target
     * @param actionId of the action
     * @return the {@link DdiCancel} response
     */
    @Operation(summary = "Cancel an action", description = """
            The Hawkbit server might cancel an operation, e.g. an unfinished update has a successor. It is up to the
            provisioning target to decide to accept the cancellation or reject it.""")
    @GetResponses
    @GetMapping(value = CONTROLLER_V1 + "/{controllerId}/" + CANCEL_ACTION + "/{actionId}",
            produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE, MEDIA_TYPE_APPLICATION_CBOR })
    ResponseEntity<DdiCancel> getControllerCancelAction(
            @PathVariable("tenant") String tenant,
            @PathVariable("controllerId") @NotEmpty String controllerId,
            @PathVariable("actionId") @NotNull Long actionId);

    /**
     * RequestMethod.POST method receiving the {@link DdiActionFeedback} from the target.
     *
     * @param feedback the {@link DdiActionFeedback} from the target.
     * @param tenant of the client
     * @param controllerId the ID of the calling target
     * @param actionId of the action we have feedback for
     * @return the {@link DdiActionFeedback} response
     */
    @Operation(summary = "Feedback channel for cancel actions", description = """
            It is up to the device how much intermediate feedback is provided. However, the action will be kept open
            until the controller on the device reports a finished (either successful or error) or rejects the action,
            e.g. the canceled actions have been started already.""")
    @PostUpdateResponses
    @ApiResponses(value = {
            @ApiResponse(responseCode = METHOD_NOT_ALLOWED_405, description = "Software module is locked", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = CONTROLLER_V1 + "/{controllerId}/" + CANCEL_ACTION + "/{actionId}/" +
            FEEDBACK, consumes = { APPLICATION_JSON_VALUE, MEDIA_TYPE_APPLICATION_CBOR })
    ResponseEntity<Void> postCancelActionFeedback(
            @Valid @RequestBody DdiActionFeedback feedback,
            @PathVariable("tenant") String tenant,
            @PathVariable("controllerId") @NotEmpty String controllerId,
            @PathVariable("actionId") @NotNull Long actionId);

    /**
     * Resource for installed distribution set to retrieve the last successfully finished action.
     *
     * @param tenant of the request
     * @param controllerId of the target
     * @param actionId of the {@link DdiDeploymentBase} that matches to installed action.
     * @param actionHistoryMessageCount specifies the number of messages to be returned from action
     *         history. Regardless of the passed value, in order to restrict resource utilization by controllers, maximum number of
     *         messages that are retrieved from database is limited by RepositoryConstants#MAX_ACTION_HISTORY_MSG_COUNT.
     *         actionHistoryMessageCount less than zero: retrieves the maximum allowed number of action status messages from history;
     *         actionHistoryMessageCount equal to zero: does not retrieve any message;
     *         actionHistoryMessageCount greater than zero: retrieves the specified number of messages, limited by maximum allowed number.
     * @return the {@link DdiDeploymentBase}. The response is of same format as for the /deploymentBase resource.
     */
    @Operation(summary = "Previously installed action", description = """
            Resource to receive information of the previous installation. Can be used to re-retrieve artifacts of
            the already finished action, for example in case a re-installation is necessary. The response will be of
            the same format as the deploymentBase operation, providing the previous action that has been finished
            successfully. As the action is already finished, no further feedback is expected.
            
            Keep in mind that the provided download links for the artifacts are generated dynamically by the update server.
            Host, port and path are not guaranteed to be similar to the provided examples below but will be defined at
            runtime.
            """)
    @GetResponses
    @GetMapping(value = CONTROLLER_V1 + "/{controllerId}/" + INSTALLED_BASE + "/{actionId}",
            produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE, MEDIA_TYPE_APPLICATION_CBOR })
    ResponseEntity<DdiDeploymentBase> getControllerInstalledAction(
            @PathVariable("tenant") String tenant,
            @PathVariable("controllerId") @NotEmpty String controllerId,
            @PathVariable("actionId") @NotNull Long actionId,
            @RequestParam(value = "actionHistory", defaultValue = NO_ACTION_HISTORY) Integer actionHistoryMessageCount);

    /**
     * Returns the confirmation base with the current auto-confirmation state for a given controllerId and toggle links. In case there are
     * actions present where the confirmation is required, a reference to it will be returned as well.
     *
     * @param tenant the controllerId is corresponding too
     * @param controllerId to check the state for
     * @return the state as {@link DdiAutoConfirmationState}
     */
    @Operation(summary = "Resource to request confirmation specific information for the controller", description = """
            Core resource for confirmation related operations. While active actions awaiting confirmation will be
            referenced, the current auto-confirmation status will be shown. In case auto-confirmation is active, details
            like the initiator, remark and date of activation (as unix timestamp) will be provided.
            Reference links to switch the auto-confirmation state are exposed as well.
            """)
    @GetResponses
    @GetMapping(value = CONTROLLER_V1 + "/{controllerId}/" + CONFIRMATION_BASE,
            produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE, MEDIA_TYPE_APPLICATION_CBOR })
    ResponseEntity<DdiConfirmationBase> getConfirmationBase(
            @PathVariable("tenant") String tenant,
            @PathVariable("controllerId") @NotEmpty String controllerId);

    /**
     * Resource for confirmation of an action.
     *
     * @param tenant of the request
     * @param controllerId of the target
     * @param actionId of the {@link DdiConfirmationBaseAction} that matches to active actions in WAITING_FOR_CONFIRMATION status.
     * @param resource a hashcode of the resource which indicates if the action has been changed, e.g. from 'soft' to 'force' and the eTag
     *         needs to be re-generated
     * @param actionHistoryMessageCount specifies the number of messages to be returned from action history. Regardless of the passed value,
     *         in order to restrict resource utilization by controllers, maximum number of messages that are retrieved from database is limited
     *         by RepositoryConstants#MAX_ACTION_HISTORY_MSG_COUNT.
     *         actionHistoryMessageCount less than zero: retrieves the maximum allowed number of action status messages from history;
     *         actionHistoryMessageCount equal to zero: does not retrieve any message;
     *         actionHistoryMessageCount greater than zero: retrieves the specified number of messages, limited by maximum allowed number.
     * @return the response
     */
    @Operation(summary = "Confirmation status of an action", description = """
            Resource to receive information about a pending confirmation. The response will be of the same format as the
            deploymentBase operation. The controller should provide feedback about the confirmation first, before
            processing the deployment.
            
            Keep in mind that the provided download links for the artifacts are generated dynamically by the update server.
            Host, port and path are not guaranteed to be similar to the provided examples below but will be defined at
            runtime.
            """)
    @GetResponses
    @GetMapping(value = CONTROLLER_V1 + "/{controllerId}/" + CONFIRMATION_BASE + "/{actionId}",
            produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE, MEDIA_TYPE_APPLICATION_CBOR })
    ResponseEntity<DdiConfirmationBaseAction> getConfirmationBaseAction(
            @PathVariable("tenant") String tenant,
            @PathVariable("controllerId") @NotEmpty String controllerId,
            @PathVariable("actionId") @NotNull Long actionId,
            @RequestParam(value = "c", required = false, defaultValue = "-1") int resource,
            @RequestParam(value = "actionHistory", defaultValue = NO_ACTION_HISTORY) Integer actionHistoryMessageCount);

    /**
     * This is the feedback channel for the {@link DdiConfirmationBaseAction} action.
     *
     * @param tenant of the client
     * @param feedback to provide
     * @param controllerId of the target that matches to controller id
     * @param actionId of the action we have feedback for
     * @return the response
     */
    @Operation(summary = "Feedback channel for actions waiting for confirmation", description = """
            The device will use this resource to either confirm or deny an action which is waiting for confirmation. The
            action will be transferred into the RUNNING state in case the device is confirming it. Afterwards it will be
            exposed by the deploymentBase.
            """)
    @PostUpdateResponses
    @ApiResponses(value = {
            @ApiResponse(responseCode = GONE_410, description = "Action is not active anymore.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = CONTROLLER_V1 + "/{controllerId}/" + CONFIRMATION_BASE + "/{actionId}/" + FEEDBACK,
            consumes = { APPLICATION_JSON_VALUE, MEDIA_TYPE_APPLICATION_CBOR })
    ResponseEntity<Void> postConfirmationActionFeedback(
            @Valid @RequestBody DdiConfirmationFeedback feedback,
            @PathVariable("tenant") String tenant,
            @PathVariable("controllerId") String controllerId,
            @PathVariable("actionId") @NotNull Long actionId);

    /**
     * Activate auto confirmation for a given controllerId. Will use the provided initiator and remark field from the provided
     * {@link DdiActivateAutoConfirmation}. If not present, the values will be prefilled with a default remark and the CONTROLLER as initiator.
     *
     * @param tenant the controllerId is corresponding too
     * @param controllerId to activate auto-confirmation for
     * @param body as {@link DdiActivateAutoConfirmation}
     * @return {@link org.springframework.http.HttpStatus#OK} if successful or {@link org.springframework.http.HttpStatus#CONFLICT} in case
     *         auto-confirmation was active already.
     */
    @Operation(summary = "Interface to activate auto-confirmation for a specific device", description = """
            The device can use this resource to activate auto-confirmation. As a result all current active as well as
            future actions will automatically be confirmed by mentioning the initiator as triggered person. Actions will
            be automatically confirmed, as long as auto-confirmation is active.
            """)
    @PostUpdateResponses
    @PostMapping(value = CONTROLLER_V1 + "/{controllerId}/" + CONFIRMATION_BASE + "/" + ACTIVATE_AUTO_CONFIRM,
            consumes = { APPLICATION_JSON_VALUE, MEDIA_TYPE_APPLICATION_CBOR })
    ResponseEntity<Void> activateAutoConfirmation(
            @PathVariable("tenant") String tenant,
            @PathVariable("controllerId") @NotEmpty String controllerId,
            @Valid @RequestBody(required = false) DdiActivateAutoConfirmation body);

    /**
     * Deactivate auto confirmation for a given controller id.
     *
     * @param tenant the controllerId is corresponding too
     * @param controllerId to disable auto-confirmation for
     * @return {@link org.springframework.http.HttpStatus#OK} if successfully executed
     */
    @Operation(summary = "Interface to deactivate auto-confirmation for a specific controller", description = """
            The device can use this resource to deactivate auto-confirmation. All active actions will remain unchanged
            while all future actions need to be confirmed, before processing with the deployment.
            """)
    @PostUpdateResponses
    @PostMapping(value = CONTROLLER_V1 + "/{controllerId}/" + CONFIRMATION_BASE + "/" + DEACTIVATE_AUTO_CONFIRM)
    ResponseEntity<Void> deactivateAutoConfirmation(
            @PathVariable("tenant") String tenant,
            @PathVariable("controllerId") @NotEmpty String controllerId);

    /**
     * Assign an already installed distribution for a target
     *
     * @param tenant of the client to provide
     * @param controllerId of the target that matches to controller id
     * @param ddiAssignedVersion as {@link DdiAssignedVersion}
     * @return the response
     */
    @Operation(summary = "Set offline assigned version", description = """
            Allow to set current running version.
            This method is EXPERIMENTAL and may change in future releases.
            """)
    @PutResponses
    @ApiResponses(value = {
            @ApiResponse(responseCode = GONE_410, description = "Action is not active anymore.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = CONTROLLER_V1 + "/{controllerId}/" + INSTALLED_BASE,
            consumes = { APPLICATION_JSON_VALUE, MEDIA_TYPE_APPLICATION_CBOR })
    ResponseEntity<Void> setAssignedOfflineVersion(
            @Valid @RequestBody DdiAssignedVersion ddiAssignedVersion,
            @PathVariable("tenant") String tenant,
            @PathVariable("controllerId") String controllerId);
}