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
 * Exception indicating that an artifact's binary does not exist anymore. This might be caused due to the soft deletion of a software module.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ArtifactBinaryNoLongerExistsException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final SpServerError THIS_ERROR = SpServerError.SP_ARTIFACT_BINARY_DELETED;

    public ArtifactBinaryNoLongerExistsException() {
        super(THIS_ERROR);
    }
}