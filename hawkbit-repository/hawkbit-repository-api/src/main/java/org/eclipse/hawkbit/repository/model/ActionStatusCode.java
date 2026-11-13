/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

/**
 * An action status code entry in the per-tenant catalogue.
 */
public interface ActionStatusCode extends NamedEntity {

    /**
     * @return the numeric status code
     */
    int getCode();

    /**
     * @return the severity level
     */
    Severity getSeverity();

    /**
     * Severity levels for action status codes.
     */
    enum Severity {
        TRACE, DEBUG, INFO, WARN, ERROR
    }
}
