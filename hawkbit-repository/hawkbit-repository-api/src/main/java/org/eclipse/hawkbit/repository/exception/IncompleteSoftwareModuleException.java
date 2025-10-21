/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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
 * Thrown if a software module is being locked while incomplete (i.e. not enough artifacts are assigned).
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class IncompleteSoftwareModuleException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    public IncompleteSoftwareModuleException() {
        super(SpServerError.SP_DS_INCOMPLETE);
    }

    public IncompleteSoftwareModuleException(final Throwable cause) {
        super(SpServerError.SP_DS_INCOMPLETE, cause);
    }

    public IncompleteSoftwareModuleException(final String message) {
        super(SpServerError.SP_DS_INCOMPLETE, message);
    }
}