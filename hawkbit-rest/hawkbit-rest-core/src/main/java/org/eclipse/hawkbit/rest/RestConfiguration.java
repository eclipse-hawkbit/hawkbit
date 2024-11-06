/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.rest.exception.MessageNotReadableException;
import org.eclipse.hawkbit.rest.exception.MultiPartFileUploadException;
import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;
import org.eclipse.hawkbit.rest.util.FileStreamingFailedException;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MultipartException;

/**
 * Configuration for Rest api.
 */
@Configuration
@EnableHypermediaSupport(type = { HypermediaType.HAL })
public class RestConfiguration {

    /**
     * {@link ControllerAdvice} for mapping {@link RuntimeException}s from the repository to {@link HttpStatus} codes.
     *
     * @return a controller advice for handling exceptions
     */
    @Bean
    ResponseExceptionHandler responseExceptionHandler() {
        return new ResponseExceptionHandler();
    }

    /**
     * Filter registration bean for spring etag filter.
     *
     * @return the spring filter registration bean for registering an etag filter in the filter chain
     */
    @Bean
    FilterRegistrationBean<ExcludePathAwareShallowETagFilter> eTagFilter() {
        final FilterRegistrationBean<ExcludePathAwareShallowETagFilter> filterRegBean = new FilterRegistrationBean<>();

        // Exclude the URLs for downloading artifacts, so no eTag is generated
        // in the ShallowEtagHeaderFilter, just using the SHA1 hash of the
        // artifact itself as 'ETag', because otherwise the file will be copied in memory!
        filterRegBean.setFilter(new ExcludePathAwareShallowETagFilter(
                "/rest/v1/softwaremodules/{smId}/artifacts/{artId}/download",
                "/{tenant}/controller/v1/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/**",
                "/api/v1/downloadserver/**"));

        return filterRegBean;
    }

    /**
     * General controller advice for exception handling.
     */
    @Slf4j
    @ControllerAdvice
    public static class ResponseExceptionHandler {

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
            ERROR_TO_HTTP_STATUS.put(SpServerError.SP_ARTIFACT_ENCRYPTION_NOT_SUPPORTED, HttpStatus.BAD_REQUEST);
            ERROR_TO_HTTP_STATUS.put(SpServerError.SP_ARTIFACT_ENCRYPTION_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
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
            ERROR_TO_HTTP_STATUS.put(SpServerError.SP_REPO_AUTO_CONFIRMATION_ALREADY_ACTIVE, HttpStatus.CONFLICT);
            ERROR_TO_HTTP_STATUS.put(SpServerError.SP_AUTO_ASSIGN_ACTION_TYPE_INVALID, HttpStatus.BAD_REQUEST);
            ERROR_TO_HTTP_STATUS.put(SpServerError.SP_CONFIGURATION_VALUE_CHANGE_NOT_ALLOWED, HttpStatus.FORBIDDEN);
            ERROR_TO_HTTP_STATUS.put(SpServerError.SP_MULTIASSIGNMENT_NOT_ENABLED, HttpStatus.BAD_REQUEST);
            ERROR_TO_HTTP_STATUS.put(SpServerError.SP_NO_WEIGHT_PROVIDED_IN_MULTIASSIGNMENT_MODE, HttpStatus.BAD_REQUEST);
            ERROR_TO_HTTP_STATUS.put(SpServerError.SP_TARGET_TYPE_IN_USE, HttpStatus.CONFLICT);
            ERROR_TO_HTTP_STATUS.put(SpServerError.SP_TARGET_TYPE_INCOMPATIBLE, HttpStatus.BAD_REQUEST);
            ERROR_TO_HTTP_STATUS.put(SpServerError.SP_TARGET_TYPE_KEY_OR_NAME_REQUIRED, HttpStatus.BAD_REQUEST);
            ERROR_TO_HTTP_STATUS.put(SpServerError.SP_DS_INVALID, HttpStatus.BAD_REQUEST);
            ERROR_TO_HTTP_STATUS.put(SpServerError.SP_DS_INCOMPLETE, HttpStatus.BAD_REQUEST);
            ERROR_TO_HTTP_STATUS.put(SpServerError.SP_LOCKED, HttpStatus.LOCKED);
            ERROR_TO_HTTP_STATUS.put(SpServerError.SP_DELETED, HttpStatus.NOT_FOUND);
            ERROR_TO_HTTP_STATUS.put(SpServerError.SP_STOP_ROLLOUT_FAILED, HttpStatus.LOCKED);
        }

        /**
         * method for handling exception of type AbstractServerRtException. Called by the Spring-Framework for exception handling.
         *
         * @param request the Http request
         * @param ex the exception which occurred
         * @return the entity to be responded containing the exception information as entity.
         */
        @ExceptionHandler(AbstractServerRtException.class)
        public ResponseEntity<ExceptionInfo> handleSpServerRtExceptions(final HttpServletRequest request, final Exception ex) {
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
         * Method for handling exception of type {@link FileStreamingFailedException} which is thrown in case the streaming of a file failed
         * due to an internal server error. As the streaming of the file has already begun, no JSON response but only the ResponseStatus 500
         * is returned.
         * Called by the Spring-Framework for exception handling.
         *
         * @param request the Http request
         * @param ex the exception which occurred
         * @return the entity to be responded containing the response status 500
         */
        @ExceptionHandler(FileStreamingFailedException.class)
        public ResponseEntity<Object> handleFileStreamingFailedException(final HttpServletRequest request, final Exception ex) {
            logRequest(request, ex);
            log.warn("File streaming failed: {}", ex.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        /**
         * Method for handling exception of type HttpMessageNotReadableException and MethodArgumentNotValidException which are thrown in case
         * the request body is not well-formed (e.g. syntax failures, missing/invalid parameters) and cannot be deserialized.
         * Called by the Spring-Framework for exception handling.
         *
         * @param request the Http request
         * @param ex the exception which occurred
         * @return the entity to be responded containing the exception information as entity.
         */
        @ExceptionHandler({
                HttpMessageNotReadableException.class,
                MethodArgumentNotValidException.class, HandlerMethodValidationException.class,
                IllegalArgumentException.class })
        public ResponseEntity<ExceptionInfo> handleExceptionCausedByIncorrectRequestBody(final HttpServletRequest request, final Exception ex) {
            logRequest(request, ex);
            final ExceptionInfo response = createExceptionInfo(new MessageNotReadableException());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        /**
         * Method for handling exception of type ConstraintViolationException which is thrown in case the request is rejected due to a
         * constraint violation.
         * Called by the Spring-Framework for exception handling.
         *
         * @param request the Http request
         * @param ex the exception which occurred
         * @return the entity to be responded containing the exception information as entity.
         */
        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ExceptionInfo> handleConstraintViolationException(final HttpServletRequest request, final ConstraintViolationException ex) {
            logRequest(request, ex);

            final ExceptionInfo response = new ExceptionInfo();
            response.setMessage(ex.getConstraintViolations().stream()
                    .map(violation -> violation.getPropertyPath() + MESSAGE_FORMATTER_SEPARATOR + violation.getMessage() + ".")
                    .collect(Collectors.joining(MESSAGE_FORMATTER_SEPARATOR)));
            response.setExceptionClass(ex.getClass().getName());
            response.setErrorCode(SpServerError.SP_REPO_CONSTRAINT_VIOLATION.getKey());

            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        /**
         * Method for handling exception of type ValidationException which is thrown in case the request is rejected due to invalid requests.
         * Called by the Spring-Framework for exception handling.
         *
         * @param request the Http request
         * @param ex the exception which occurred
         * @return the entity to be responded containing the exception information as entity.
         */
        @ExceptionHandler(ValidationException.class)
        public ResponseEntity<ExceptionInfo> handleValidationException(final HttpServletRequest request, final ValidationException ex) {
            logRequest(request, ex);

            final ExceptionInfo response = new ExceptionInfo();
            response.setMessage(ex.getMessage());
            response.setExceptionClass(ex.getClass().getName());
            response.setErrorCode(SpServerError.SP_REPO_CONSTRAINT_VIOLATION.getKey());

            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        /**
         * Method for handling exception of type {@link MultipartException} which is thrown in case the request body is not well-formed and
         * cannot be deserialized.
         * Called by the Spring-Framework for exception handling.
         *
         * @param request the Http request
         * @param ex the exception which occurred
         * @return the entity to be responded containing the exception information as entity.
         */
        @ExceptionHandler(MultipartException.class)
        public ResponseEntity<ExceptionInfo> handleMultipartException(final HttpServletRequest request, final Exception ex) {
            logRequest(request, ex);

            final List<Throwable> throwables = ExceptionUtils.getThrowableList(ex);
            final Throwable responseCause = throwables.get(throwables.size() - 1);

            if (ObjectUtils.isEmpty(responseCause.getMessage())) {
                log.warn("Request {} lead to MultipartException without root cause message:\n{}", request.getRequestURL(),
                        ex.getStackTrace());
            }

            return new ResponseEntity<>(createExceptionInfo(new MultiPartFileUploadException(responseCause)), HttpStatus.BAD_REQUEST);
        }

        private static HttpStatus getStatusOrDefault(final SpServerError error) {
            return ERROR_TO_HTTP_STATUS.getOrDefault(error, DEFAULT_RESPONSE_STATUS);
        }

        private void logRequest(final HttpServletRequest request, final Exception ex) {
            log.debug("Handling exception {} of request {}", ex.getClass().getName(), request.getRequestURL());
        }

        private ExceptionInfo createExceptionInfo(final Exception ex) {
            final ExceptionInfo response = new ExceptionInfo();
            response.setMessage(ex.getMessage());
            response.setExceptionClass(ex.getClass().getName());
            if (ex instanceof AbstractServerRtException) {
                response.setErrorCode(((AbstractServerRtException) ex).getError().getKey());
                response.setInfo(((AbstractServerRtException) ex).getInfo());
            }
            return response;
        }

    }

    /**
     * An {@link ShallowEtagHeaderFilter} with exclusion paths to exclude some paths
     * where no ETag header should be generated due that calculating the ETag is an
     * expensive operation and the response output need to be copied in memory which
     * should be excluded in case of artifact downloads which could be big of size.
     */
    static class ExcludePathAwareShallowETagFilter extends ShallowEtagHeaderFilter {

        private final String[] excludeAntPaths;
        private final AntPathMatcher antMatcher = new AntPathMatcher();

        /**
         * @param excludeAntPaths
         */
        public ExcludePathAwareShallowETagFilter(final String... excludeAntPaths) {
            this.excludeAntPaths = excludeAntPaths;
        }

        @Override
        protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
                throws ServletException, IOException {
            final boolean shouldExclude = shouldExclude(request);
            if (shouldExclude) {
                filterChain.doFilter(request, response);
            } else {
                super.doFilterInternal(request, response, filterChain);
            }
        }

        private boolean shouldExclude(final HttpServletRequest request) {
            for (final String pattern : excludeAntPaths) {
                if (antMatcher.match(request.getContextPath() + pattern, request.getRequestURI())) {
                    // exclude this request from eTag filter
                    return true;
                }
            }
            return false;
        }
    }

}