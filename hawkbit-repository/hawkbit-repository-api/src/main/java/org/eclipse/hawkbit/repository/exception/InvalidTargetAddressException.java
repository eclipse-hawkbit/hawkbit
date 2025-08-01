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
 * Exception which is thrown when trying to set an invalid target address.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InvalidTargetAddressException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * @param message the message for this exception
     */
    public InvalidTargetAddressException(final String message) {
        super(message, SpServerError.SP_REPO_INVALID_TARGET_ADDRESS);
    }

    /**
     * @param message the message for this exception
     * @param cause the cause for this exception
     */
    public InvalidTargetAddressException(final String message, final Throwable cause) {
        super(message, SpServerError.SP_REPO_INVALID_TARGET_ADDRESS, cause);
    }
}
