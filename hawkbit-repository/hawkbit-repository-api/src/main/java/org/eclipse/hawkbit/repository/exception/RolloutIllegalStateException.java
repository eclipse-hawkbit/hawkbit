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
 * The {@link RolloutIllegalStateException} is thrown when a rollout is changing its state which is not valid. E.g. trying to start an already
 * running rollout, or trying to resume an already finished rollout.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RolloutIllegalStateException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final SpServerError THIS_ERROR = SpServerError.SP_ROLLOUT_ILLEGAL_STATE;

    public RolloutIllegalStateException(final String message) {
        super(THIS_ERROR, message);
    }
}