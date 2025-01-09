/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource.deprecated;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.resource.deprecated.json.model.MgmtAssignedDistributionSetRequestBody;
import org.eclipse.hawkbit.mgmt.rest.resource.deprecated.json.model.MgmtAssignedTargetRequestBody;
import org.eclipse.hawkbit.mgmt.rest.resource.deprecated.json.model.MgmtDistributionSetTagAssigmentResult;
import org.eclipse.hawkbit.mgmt.rest.resource.deprecated.json.model.MgmtTargetTagAssigmentResult;
import org.eclipse.hawkbit.rest.OpenApiConfiguration;
import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * REST Resource handling for DistributionSetTag CRUD operations.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Deprecated(forRemoval = true)
@Tag(name = "Deprecated operations", description = "Deprecated REST operations.",
        extensions = @Extension(name = OpenApiConfiguration.X_HAWKBIT, properties = @ExtensionProperty(name = "order", value = "2147483647")))
public interface DeprecatedMgmtRestApi {

    /**
     * Handles the POST request to toggle the assignment of distribution sets by
     * the given tag id.</br>
     * From {@link org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTagRestApi}
     *
     * @param distributionsetTagId the ID of the distribution set tag to retrieve
     * @param assignedDSRequestBodies list of distribution set ids to be toggled
     * @return the list of assigned distribution sets and unassigned distribution sets.
     * @deprecated since 0.6.0 with toggle assignment deprecation
     */
    @Operation(summary = "[DEPRECATED] Toggle the assignment of distribution sets by the given tag id",
            description = "Handles the POST request of toggle distribution assignment. The request body must always be a list of distribution " +
                    "set ids.",
            deprecated = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403",
                    description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                            "data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another " +
                    "user in another request at the same time. You may retry your modification request.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not " +
                    "supported by the server for this resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = MgmtRestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING +
            MgmtRestConstants.DISTRIBUTIONSET_TAG_DISTRIBUTIONSETS_REQUEST_MAPPING + "/toggleTagAssignment")
    @Deprecated(forRemoval = true, since = "0.6.0")
    ResponseEntity<MgmtDistributionSetTagAssigmentResult> toggleDistributionSetTagAssignment(
            @PathVariable("distributionsetTagId") Long distributionsetTagId,
            @RequestBody List<MgmtAssignedDistributionSetRequestBody> assignedDSRequestBodies);

    /**
     * Handles the POST request to assign distribution sets to the given tag id..</br>
     * From {@link org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTagRestApi}
     *
     * @param distributionsetTagId the ID of the distribution set tag to retrieve
     * @param assignedDSRequestBodies list of distribution sets ids to be assigned
     * @return the list of assigned distribution set.
     * @deprecated since 0.6.0 in favor or assign by ds ids
     */
    @Operation(summary = "[DEPRECATED] Assign distribution sets to the given tag id",
            description = "Handles the POST request of distribution assignment. Already assigned distribution will be ignored.",
            deprecated = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403",
                    description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                            "data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another " +
                    "user in another request at the same time. You may retry your modification request.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not " +
                    "supported by the server for this resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = MgmtRestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING + MgmtRestConstants.DISTRIBUTIONSET_TAG_DISTRIBUTIONSETS_REQUEST_MAPPING,
            consumes = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE },
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    @Deprecated(forRemoval = true, since = "0.6.0")
    ResponseEntity<List<MgmtDistributionSet>> assignDistributionSetsByRequestBody(
            @PathVariable("distributionsetTagId") Long distributionsetTagId,
            @RequestBody List<MgmtAssignedDistributionSetRequestBody> assignedDSRequestBodies);

    /**
     * Handles the POST request to toggle the assignment of targets by the given tag id.<br/>
     * From {@link org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetTagRestApi}
     *
     * @param targetTagId the ID of the target tag to retrieve
     * @param assignedTargetRequestBodies list of controller ids to be toggled
     * @return the list of assigned targets and unassigned targets.
     * @deprecated since 0.6.0 - not very usable with very unclear logic
     */
    @Operation(summary = "[DEPRECATED] Toggles target tag assignment", description = "Handles the POST request of toggle target " +
            "assignment. The request body must always be a list of controller ids.",
            deprecated = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies."),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another " +
                    "user in another request at the same time. You may retry your modification request."),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not " +
                    "supported by the server for this resource."),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
    @PostMapping(value = MgmtRestConstants.TARGET_TAG_V1_REQUEST_MAPPING +
            MgmtRestConstants.TARGET_TAG_TARGETS_REQUEST_MAPPING + "/toggleTagAssignment",
            consumes = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE },
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    @Deprecated(forRemoval = true, since = "0.6.0")
    ResponseEntity<MgmtTargetTagAssigmentResult> toggleTargetTagAssignment(
            @PathVariable("targetTagId") Long targetTagId,
            @RequestBody List<MgmtAssignedTargetRequestBody> assignedTargetRequestBodies);

    /**
     * Handles the POST request to assign targets to the given tag id.<br/>
     * From {@link org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetTagRestApi}
     *
     * @param targetTagId the ID of the target tag to retrieve
     * @param assignedTargetRequestBodies list of controller ids to be assigned
     * @return the list of assigned targets.
     * @deprecated since 0.6.0 in favour of {@link org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetTagRestApi#assignTargets}
     */
    @Operation(summary = "[DEPRECATED] Assign target(s) to given tagId and return targets",
            description = "Handles the POST request of target assignment. Already assigned target will be ignored.",
            deprecated = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully assigned"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies."),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another " +
                    "user in another request at the same time. You may retry your modification request."),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not " +
                    "supported by the server for this resource."),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.")
    })
    @PostMapping(value = MgmtRestConstants.TARGET_TAG_V1_REQUEST_MAPPING + MgmtRestConstants.TARGET_TAG_TARGETS_REQUEST_MAPPING,
            consumes = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE },
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    @Deprecated(forRemoval = true, since = "0.6.0")
    ResponseEntity<List<MgmtTarget>> assignTargetsByRequestBody(
            @PathVariable("targetTagId") Long targetTagId,
            @RequestBody List<MgmtAssignedTargetRequestBody> assignedTargetRequestBodies);
}