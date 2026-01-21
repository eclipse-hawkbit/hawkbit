/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;

/**
 * Standard API response annotations for REST endpoints. <br/>
 * Contains composed annotations for different HTTP method types:
 * <ul>
 *     <li>POST - for create and update, </li>
 *     <li>GET - with 404 on not exist or 200 on not exist (if exist)</li>
 *     <li>PUT - with 200 and 204 (NoContent) flavours </li>
 *     <li>DELETE</li>
 * </ul>
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ApiResponsesConstants {

    // Response Codes
    private static final String OK_200 = "200";
    private static final String CREATED_201 = "201";
    private static final String NO_CONTENT_204 = "204";
    private static final String BAD_REQUEST_400 = "400";
    private static final String UNAUTHORIZED_401 = "401";
    private static final String FORBIDDEN_403 = "403";
    private static final String NOT_FOUND_404 = "404";
    private static final String METHOD_NOT_ALLOWED_405 = "405";
    private static final String NOT_ACCEPTABLE_406 = "406";
    private static final String CONFLICT_409 = "409";
    private static final String UNSUPPORTED_MEDIA_TYPE_415 = "415";
    private static final String TOO_MANY_REQUESTS_429 = "429";

    // Success Descriptions
    private static final String DESC_SUCCESS_CREATED = "Successfully created";
    private static final String DESC_SUCCESS_RETRIEVED = "Successfully retrieved";
    private static final String DESC_SUCCESS_UPDATED = "Successfully updated";
    private static final String DESC_SUCCESS_DELETED = "Successfully deleted";

    // Error Descriptions
    private static final String DESC_BAD_REQUEST = "Bad Request - e.g. invalid parameters";
    private static final String DESC_UNAUTHORIZED = "The request requires user auth.";
    private static final String DESC_FORBIDDEN = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.";
    private static final String DESC_NOT_FOUND = "Requested resource not found.";
    private static final String DESC_REC_OR_REF_NOT_FOUND = "Requested (or referenced) resource not found.";
    private static final String DESC_METHOD_NOT_ALLOWED = "The http request method is not allowed on the resource.";
    private static final String DESC_NOT_ACCEPTABLE = "In case accept header is specified and not application/json.";
    private static final String DESC_CONFLICT = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.";
    private static final String DESC_UNSUPPORTED_MEDIA_TYPE = "The request was attempt with a media-type which is not supported by the server for this resource.";
    private static final String DESC_TOO_MANY_REQUESTS = "Too many requests. The server will refuse further attempts and the client has to wait another second.";

    // Media Types
    private static final String MEDIA_TYPE_JSON = "application/json";

    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses(value = {
            @ApiResponse(responseCode = CREATED_201, description = DESC_SUCCESS_CREATED),
            @ApiResponse(
                    responseCode = CONFLICT_409,
                    description = DESC_CONFLICT,
                    content = @Content(mediaType = MEDIA_TYPE_JSON, schema = @Schema(hidden = true))),
            @ApiResponse(
                    responseCode = UNSUPPORTED_MEDIA_TYPE_415,
                    description = DESC_UNSUPPORTED_MEDIA_TYPE,
                    content = @Content(mediaType = MEDIA_TYPE_JSON, schema = @Schema(hidden = true)))
    })
    @CommonErrorResponses
    public @interface PostCreateResponses {
    }

    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = DESC_SUCCESS_UPDATED),
            @ApiResponse(
                    responseCode = NOT_FOUND_404, description = DESC_REC_OR_REF_NOT_FOUND,
                    content = @Content(mediaType = MEDIA_TYPE_JSON, schema = @Schema(hidden = true))),
            @ApiResponse(
                    responseCode = CONFLICT_409, description = DESC_CONFLICT,
                    content = @Content(mediaType = MEDIA_TYPE_JSON, schema = @Schema(hidden = true))),
            @ApiResponse(
                    responseCode = UNSUPPORTED_MEDIA_TYPE_415,
                    description = DESC_UNSUPPORTED_MEDIA_TYPE,
                    content = @Content(mediaType = MEDIA_TYPE_JSON, schema = @Schema(hidden = true)))
    })
    @CommonErrorResponses
    public @interface PostUpdateResponses {
    }

    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses(value = {
            @ApiResponse(responseCode = NO_CONTENT_204, description = DESC_SUCCESS_UPDATED),
            @ApiResponse(
                    responseCode = CONFLICT_409,
                    description = DESC_CONFLICT,
                    content = @Content(mediaType = MEDIA_TYPE_JSON, schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = UNSUPPORTED_MEDIA_TYPE_415, description = DESC_UNSUPPORTED_MEDIA_TYPE,
                    content = @Content(mediaType = MEDIA_TYPE_JSON, schema = @Schema(hidden = true)))
    })
    @CommonErrorResponses
    public @interface PostCreateNoContentResponses {
    }

    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses(value = {
            @ApiResponse(responseCode = NO_CONTENT_204, description = DESC_SUCCESS_UPDATED),
            @ApiResponse(
                    responseCode = NOT_FOUND_404, description = DESC_REC_OR_REF_NOT_FOUND,
                    content = @Content(mediaType = MEDIA_TYPE_JSON, schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = UNSUPPORTED_MEDIA_TYPE_415, description = DESC_UNSUPPORTED_MEDIA_TYPE,
                    content = @Content(mediaType = MEDIA_TYPE_JSON, schema = @Schema(hidden = true)))
    })
    @CommonErrorResponses
    public @interface PostUpdateNoContentResponses {
    }

    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = DESC_SUCCESS_RETRIEVED),
            @ApiResponse(
                    responseCode = NOT_FOUND_404, description = DESC_NOT_FOUND,
                    content = @Content(mediaType = MEDIA_TYPE_JSON, schema = @Schema(hidden = true)))
    })
    @CommonErrorResponses
    public @interface GetResponses {
    }

    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = DESC_SUCCESS_RETRIEVED)
    })
    @CommonErrorResponses
    public @interface GetIfExistResponses {
    }

    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK_200, description = DESC_SUCCESS_UPDATED),
            @ApiResponse(
                    responseCode = NOT_FOUND_404, description = DESC_REC_OR_REF_NOT_FOUND,
                    content = @Content(mediaType = MEDIA_TYPE_JSON, schema = @Schema(hidden = true))),
            @ApiResponse(
                    responseCode = CONFLICT_409,
                    description = DESC_CONFLICT,
                    content = @Content(mediaType = MEDIA_TYPE_JSON, schema = @Schema(hidden = true))),
            @ApiResponse(
                    responseCode = UNSUPPORTED_MEDIA_TYPE_415,
                    description = DESC_UNSUPPORTED_MEDIA_TYPE,
                    content = @Content(mediaType = MEDIA_TYPE_JSON, schema = @Schema(hidden = true)))
    })
    @CommonErrorResponses
    public @interface PutResponses {
    }

    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses(value = {
            @ApiResponse(responseCode = NO_CONTENT_204, description = DESC_SUCCESS_UPDATED),
            @ApiResponse(
                    responseCode = NOT_FOUND_404, description = DESC_REC_OR_REF_NOT_FOUND,
                    content = @Content(mediaType = MEDIA_TYPE_JSON, schema = @Schema(hidden = true))),
            @ApiResponse(
                    responseCode = CONFLICT_409,
                    description = DESC_CONFLICT,
                    content = @Content(mediaType = MEDIA_TYPE_JSON, schema = @Schema(hidden = true))),
            @ApiResponse(
                    responseCode = UNSUPPORTED_MEDIA_TYPE_415,
                    description = DESC_UNSUPPORTED_MEDIA_TYPE,
                    content = @Content(mediaType = MEDIA_TYPE_JSON, schema = @Schema(hidden = true)))
    })
    @CommonErrorResponses
    public @interface PutNoContentResponses {
    }

    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses(value = {
            @ApiResponse(responseCode = NO_CONTENT_204, description = DESC_SUCCESS_DELETED),
            @ApiResponse(
                    responseCode = NOT_FOUND_404, description = DESC_NOT_FOUND,
                    content = @Content(mediaType = MEDIA_TYPE_JSON, schema = @Schema(hidden = true)))
    })
    @CommonErrorResponses
    public @interface DeleteResponses {
    }

    @Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = BAD_REQUEST_400, description = DESC_BAD_REQUEST,
                    content = @Content(mediaType = MEDIA_TYPE_JSON, schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(
                    responseCode = UNAUTHORIZED_401, description = DESC_UNAUTHORIZED,
                    content = @Content(mediaType = MEDIA_TYPE_JSON, schema = @Schema(hidden = true))),
            @ApiResponse(
                    responseCode = FORBIDDEN_403,
                    description = DESC_FORBIDDEN,
                    content = @Content(mediaType = MEDIA_TYPE_JSON, schema = @Schema(hidden = true))),
            @ApiResponse(
                    responseCode = METHOD_NOT_ALLOWED_405, description = DESC_METHOD_NOT_ALLOWED,
                    content = @Content(mediaType = MEDIA_TYPE_JSON, schema = @Schema(hidden = true))),
            @ApiResponse(
                    responseCode = NOT_ACCEPTABLE_406, description = DESC_NOT_ACCEPTABLE,
                    content = @Content(mediaType = MEDIA_TYPE_JSON, schema = @Schema(hidden = true))),
            @ApiResponse(
                    responseCode = TOO_MANY_REQUESTS_429,
                    description = DESC_TOO_MANY_REQUESTS,
                    content = @Content(mediaType = MEDIA_TYPE_JSON, schema = @Schema(hidden = true)))
    })
    private @interface CommonErrorResponses {
    }
}