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
 * Thrown if a distribution set is assigned to a target that is incomplete (i.e. mandatory modules are missing).
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class IncompleteDistributionSetException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    public IncompleteDistributionSetException() {
        super(SpServerError.SP_DS_INCOMPLETE);
    }

    public IncompleteDistributionSetException(final Throwable cause) {
        super(SpServerError.SP_DS_INCOMPLETE, cause);
    }

    public IncompleteDistributionSetException(final String message) {
        super(SpServerError.SP_DS_INCOMPLETE, message);
    }
}