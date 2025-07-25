/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.exception;

import java.io.Serial;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * The {@link TenantNotExistException} is thrown when e.g. a controller tries to register itself at SP as plug'n play target and the tenant
 * specified in the URL for this target does not exist. To avoid that targets could register automatically new tenants.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TenantNotExistException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final SpServerError THIS_ERROR = SpServerError.SP_REPO_TENANT_NOT_EXISTS;

    /**
     * Default constructor.
     */
    public TenantNotExistException() {
        super(THIS_ERROR);
    }

    /**
     * Parameterized constructor.
     *
     * @param cause of the exception
     */
    public TenantNotExistException(final Throwable cause) {
        super(THIS_ERROR, cause);
    }

    /**
     * Parameterized constructor.
     *
     * @param message of the exception
     * @param cause of the exception
     */
    public TenantNotExistException(final String message, final Throwable cause) {
        super(message, THIS_ERROR, cause);
    }

    /**
     * Parameterized constructor.
     *
     * @param message of the exception
     */
    public TenantNotExistException(final String message) {
        super(message, THIS_ERROR);
    }
}
