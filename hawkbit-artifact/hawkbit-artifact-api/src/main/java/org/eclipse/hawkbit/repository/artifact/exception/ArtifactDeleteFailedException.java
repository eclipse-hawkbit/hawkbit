/**
 * Copyright (c) 2025 Bosch Digital GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.artifact.exception;

import java.io.Serial;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * Thrown if artifact deletion failed.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class ArtifactDeleteFailedException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ArtifactDeleteFailedException(final Throwable cause) {
        super(SpServerError.SP_ARTIFACT_DELETE_FAILED, cause);
    }
}