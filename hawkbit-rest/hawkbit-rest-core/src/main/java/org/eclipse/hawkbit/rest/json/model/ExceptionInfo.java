/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.json.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * A exception model rest representation with JSON annotations for response
 * bodies in case of RESTful exception occurrence.
 *
 */
@JsonInclude(Include.NON_EMPTY)
public class ExceptionInfo {

    private String exceptionClass;
    private String errorCode;
    private String message;
    private List<String> parameters;

    /**
     * @return the exceptionClass
     */
    public String getExceptionClass() {
        return exceptionClass;
    }

    /**
     * @param exceptionClass
     *            the exceptionClass to set
     */
    public void setExceptionClass(final String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    /**
     * @return the parameters
     */
    public List<String> getParameters() {
        return parameters;
    }

    /**
     * @param parameters
     *            the parameters to set
     */
    public void setParameters(final List<String> parameters) {
        this.parameters = parameters;
    }

    /**
     * @return the errorCode
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * @param errorCode
     *            the errorCode to set
     */
    public void setErrorCode(final String errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message
     *            the message to set
     */
    public void setMessage(final String message) {
        this.message = message;
    }
}
