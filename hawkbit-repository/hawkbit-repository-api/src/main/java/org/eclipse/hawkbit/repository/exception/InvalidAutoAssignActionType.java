/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.repository.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

public class InvalidAutoAssignActionType extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;
    private static final SpServerError THIS_ERROR = SpServerError.SP_AUTO_ASSIGN_ACTION_TYPE_INVALID;

    /**
     * Default constructor.
     */
    public InvalidAutoAssignActionType() {
        super(THIS_ERROR);
    }

    /**
     * Parameterized constructor.
     * 
     * @param message
     *            of the exception
     */
    public InvalidAutoAssignActionType(final String message) {
        super(message, THIS_ERROR);
    }

}
