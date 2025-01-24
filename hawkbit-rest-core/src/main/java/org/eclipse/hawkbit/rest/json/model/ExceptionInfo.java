/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest.json.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;

/**
 * An exception model rest representation with JSON annotations for response bodies in case of RESTful exception occurrence.
 */
@Data
@JsonInclude(Include.NON_EMPTY)
public class ExceptionInfo {

    private String exceptionClass;
    private String errorCode;
    private String message;
    private Map<String, Object> info;
}
