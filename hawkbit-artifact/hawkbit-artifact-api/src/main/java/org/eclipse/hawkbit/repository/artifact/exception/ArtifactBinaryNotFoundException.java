/**
 * Copyright (c) 2025 Bosch Digital GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.artifact.exception;

import java.io.Serial;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class ArtifactBinaryNotFoundException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ArtifactBinaryNotFoundException(final String message) {
        super(SpServerError.SP_ARTIFACT_LOAD_FAILED, message);
    }
}