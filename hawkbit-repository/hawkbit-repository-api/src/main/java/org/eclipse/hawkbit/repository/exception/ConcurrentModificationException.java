/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
 * Thrown when an entity cannot be stored because it was modified concurrently in another session (optimistic locking conflict).
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ConcurrentModificationException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ConcurrentModificationException(final Throwable cause) {
        super(SpServerError.SP_REPO_CONCURRENT_MODIFICATION, cause);
    }
}
