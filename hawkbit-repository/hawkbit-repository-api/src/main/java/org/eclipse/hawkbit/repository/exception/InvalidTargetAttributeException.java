/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others
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

public class InvalidTargetAttributeException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final SpServerError THIS_ERROR = SpServerError.SP_TARGET_ATTRIBUTES_INVALID;

    /**
     * Default constructor.
     */
    public InvalidTargetAttributeException() {
        super(THIS_ERROR);
    }
}
