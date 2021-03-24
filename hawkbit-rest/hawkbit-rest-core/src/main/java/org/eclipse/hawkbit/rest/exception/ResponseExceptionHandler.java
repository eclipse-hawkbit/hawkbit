/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.exception;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;
import org.eclipse.hawkbit.rest.util.FileStreamingFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;

import com.google.common.collect.Iterables;

/**
 * General controller advice for exception handling.
 */
@ControllerAdvice
public class ResponseExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ResponseExceptionHandler.class);
    private static final Map<SpServerError, HttpStatus> ERROR_TO_HTTP_STATUS = new EnumMap<>(SpServerError.class);
    private static final HttpStatus DEFAULT_RESPONSE_STATUS = HttpStatus.INTERNAL_SERVER_ERROR;

    private static final String MESSAGE_FORMATTER_SEPARATOR = " ";

    static {
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_REPO_ENTITY_NOT_EXISTS, HttpStatus.NOT_FOUND);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_REPO_ENTITY_ALREADY_EXISTS, HttpStatus.CONFLICT);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_REPO_ENTITY_READ_ONLY, HttpStatus.FORBIDDEN);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_REST_SORT_PARAM_INVALID_DIRECTION, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_REST_SORT_PARAM_INVALID_FIELD, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_REST_SORT_PARAM_SYNTAX, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_REST_RSQL_PARAM_INVALID_FIELD, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_REST_RSQL_SEARCH_PARAM_SYNTAX, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_INSUFFICIENT_PERMISSION, HttpStatus.FORBIDDEN);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_ARTIFACT_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_ARTIFACT_UPLOAD_FAILED_SHA1_MATCH, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_ARTIFACT_UPLOAD_FAILED_SHA256_MATCH, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_ARTIFACT_UPLOAD_FAILED_MD5_MATCH, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_ARTIFACT_DELETE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_ARTIFACT_BINARY_DELETED, HttpStatus.GONE);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_ARTIFACT_LOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_QUOTA_EXCEEDED, HttpStatus.FORBIDDEN);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_FILE_SIZE_QUOTA_EXCEEDED, HttpStatus.FORBIDDEN);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_STORAGE_QUOTA_EXCEEDED, HttpStatus.FORBIDDEN);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_ACTION_NOT_CANCELABLE, HttpStatus.METHOD_NOT_ALLOWED);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_ACTION_NOT_FORCE_QUITABLE, HttpStatus.METHOD_NOT_ALLOWED);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_DS_CREATION_FAILED_MISSING_MODULE, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_DS_MODULE_UNSUPPORTED, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_DS_TYPE_UNDEFINED, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_REPO_TENANT_NOT_EXISTS, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_ENTITY_LOCKED, HttpStatus.LOCKED);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_ROLLOUT_ILLEGAL_STATE, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_CONFIGURATION_VALUE_INVALID, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_CONFIGURATION_KEY_INVALID, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_REPO_INVALID_TARGET_ADDRESS, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_REPO_CONSTRAINT_VIOLATION, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_REPO_OPERATION_NOT_SUPPORTED, HttpStatus.GONE);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_REPO_CONCURRENT_MODIFICATION, HttpStatus.CONFLICT);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_MAINTENANCE_SCHEDULE_INVALID, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_TARGET_ATTRIBUTES_INVALID, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_AUTO_ASSIGN_ACTION_TYPE_INVALID, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_AUTO_ASSIGN_DISTRIBUTION_SET_INVALID, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_CONFIGURATION_VALUE_CHANGE_NOT_ALLOWED, HttpStatus.FORBIDDEN);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_MULTIASSIGNMENT_NOT_ENABLED, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_NO_WEIGHT_PROVIDED_IN_MULTIASSIGNMENT_MODE, HttpStatus.BAD_REQUEST);
    }

    private static HttpStatus getStatusOrDefault(final SpServerError error) {
        return ERROR_TO_HTTP_STATUS.getOrDefault(error, DEFAULT_RESPONSE_STATUS);
    }

    /**
     * method for handling exception of type AbstractServerRtException. Called
     * by the Spring-Framework for exception handling.
     *
     * @param request
     *            the Http request
     * @param ex
     *            the exception which occurred
     *
     * @return the entity to be responded containing the exception information
     *         as entity.
     */
    @ExceptionHandler(AbstractServerRtException.class)
    public ResponseEntity<ExceptionInfo> handleSpServerRtExceptions(final HttpServletRequest request,
            final Exception ex) {
        logRequest(request, ex);
        final ExceptionInfo response = createExceptionInfo(ex);
        final HttpStatus responseStatus;
        if (ex instanceof AbstractServerRtException) {
            responseStatus = getStatusOrDefault(((AbstractServerRtException) ex).getError());
        } else {
            responseStatus = DEFAULT_RESPONSE_STATUS;
        }
        return new ResponseEntity<>(response, responseStatus);
    }

    /**
     * Method for handling exception of type
     * {@link FileStreamingFailedException} which is thrown in case the
     * streaming of a file failed due to an internal server error. As the
     * streaming of the file has already begun, no JSON response but only the
     * ResponseStatus 500 is returned. Called by the Spring-Framework for
     * exception handling.
     *
     * @param request
     *            the Http request
     * @param ex
     *            the exception which occurred
     * @return the entity to be responded containing the response status 500
     */
    @ExceptionHandler(FileStreamingFailedException.class)
    public ResponseEntity<Object> handleFileStreamingFailedException(final HttpServletRequest request,
            final Exception ex) {
        logRequest(request, ex);
        LOG.warn("File streaming failed: {}", ex.getMessage());
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Method for handling exception of type HttpMessageNotReadableException and
     * MethodArgumentNotValidException which are thrown in case the request body
     * is not well formed (e.g. syntax failures, missing/invalid parameters) and
     * cannot be deserialized. Called by the Spring-Framework for exception
     * handling.
     *
     * @param request
     *            the Http request
     * @param ex
     *            the exception which occurred
     * @return the entity to be responded containing the exception information
     *         as entity.
     */
    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentNotValidException.class })
    public ResponseEntity<ExceptionInfo> handleExceptionCausedByIncorrectRequestBody(final HttpServletRequest request,
            final Exception ex) {
        logRequest(request, ex);
        final ExceptionInfo response = createExceptionInfo(new MessageNotReadableException());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Method for handling exception of type ConstraintViolationException which
     * is thrown in case the request is rejected due to a constraint violation.
     * Called by the Spring-Framework for exception handling.
     *
     * @param request
     *            the Http request
     * @param ex
     *            the exception which occurred
     * @return the entity to be responded containing the exception information
     *         as entity.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ExceptionInfo> handleConstraintViolationException(final HttpServletRequest request,
            final ConstraintViolationException ex) {
        logRequest(request, ex);

        final ExceptionInfo response = new ExceptionInfo();
        response.setMessage(ex.getConstraintViolations().stream().map(
                violation -> violation.getPropertyPath() + MESSAGE_FORMATTER_SEPARATOR + violation.getMessage() + ".")
                .collect(Collectors.joining(MESSAGE_FORMATTER_SEPARATOR)));
        response.setExceptionClass(ex.getClass().getName());
        response.setErrorCode(SpServerError.SP_REPO_CONSTRAINT_VIOLATION.getKey());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Method for handling exception of type ValidationException which is thrown
     * in case the request is rejected due to invalid requests. Called by the
     * Spring-Framework for exception handling.
     *
     * @param request
     *            the Http request
     * @param ex
     *            the exception which occurred
     * @return the entity to be responded containing the exception information
     *         as entity.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ExceptionInfo> handleValidationException(final HttpServletRequest request,
            final ValidationException ex) {
        logRequest(request, ex);

        final ExceptionInfo response = new ExceptionInfo();
        response.setMessage(ex.getMessage());
        response.setExceptionClass(ex.getClass().getName());
        response.setErrorCode(SpServerError.SP_REPO_CONSTRAINT_VIOLATION.getKey());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Method for handling exception of type {@link MultipartException} which is
     * thrown in case the request body is not well formed and cannot be
     * deserialized. Called by the Spring-Framework for exception handling.
     *
     * @param request
     *            the Http request
     * @param ex
     *            the exception which occurred
     * @return the entity to be responded containing the exception information
     *         as entity.
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ExceptionInfo> handleMultipartException(final HttpServletRequest request,
            final Exception ex) {

        logRequest(request, ex);

        final List<Throwable> throwables = ExceptionUtils.getThrowableList(ex);
        final Throwable responseCause = Iterables.getLast(throwables);

        if (responseCause.getMessage().isEmpty()) {
            LOG.warn("Request {} lead to MultipartException without root cause message:\n{}", request.getRequestURL(),
                    ex.getStackTrace());
        }

        final ExceptionInfo response = createExceptionInfo(new MultiPartFileUploadException(responseCause));
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    private void logRequest(final HttpServletRequest request, final Exception ex) {
        LOG.debug("Handling exception {} of request {}", ex.getClass().getName(), request.getRequestURL());
    }

    private ExceptionInfo createExceptionInfo(final Exception ex) {
        final ExceptionInfo response = new ExceptionInfo();
        response.setMessage(ex.getMessage());
        response.setExceptionClass(ex.getClass().getName());
        if (ex instanceof AbstractServerRtException) {
            response.setErrorCode(((AbstractServerRtException) ex).getError().getKey());
        }

        return response;
    }

}
