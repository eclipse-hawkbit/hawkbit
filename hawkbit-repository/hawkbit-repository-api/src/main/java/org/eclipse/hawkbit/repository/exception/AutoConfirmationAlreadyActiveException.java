/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.exception;

import java.io.Serial;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * The {@link AutoConfirmationAlreadyActiveException} is thrown when auto
 * confirmation is already active for a device but the
 * {@link org.eclipse.hawkbit.repository.ConfirmationManagement#activateAutoConfirmation}
 * is getting called.
 */
public class AutoConfirmationAlreadyActiveException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final SpServerError THIS_ERROR = SpServerError.SP_REPO_AUTO_CONFIRMATION_ALREADY_ACTIVE;

    /**
     * Default constructor.
     */
    public AutoConfirmationAlreadyActiveException() {
        super(THIS_ERROR);
    }

    /**
     * Parameterized constructor.
     *
     * @param cause of the exception
     */
    public AutoConfirmationAlreadyActiveException(final Throwable cause) {
        super(THIS_ERROR, cause);
    }

    /**
     * Parameterized constructor for auto confirmation is already active for given
     * controller ID
     *
     * @param controllerId of affected device
     */
    public AutoConfirmationAlreadyActiveException(final String controllerId) {
        super("Auto confirmation is already active for device " + controllerId, THIS_ERROR);
    }

}
