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
 * Thrown when force quitting an actions is not allowed. e.g. the action is not active, or it is not canceled before.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class ForceQuitActionNotAllowedException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ForceQuitActionNotAllowedException(final String message) {
        super(SpServerError.SP_ACTION_NOT_FORCE_QUITABLE, message);
    }
}