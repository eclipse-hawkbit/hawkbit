/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
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
 * Thrown if SHA256 checksum check fails.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InvalidSha256HashException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidSha256HashException(final String message, final Throwable cause) {
        super(SpServerError.SP_ARTIFACT_UPLOAD_FAILED_SHA256_MATCH, message, cause);
    }
}