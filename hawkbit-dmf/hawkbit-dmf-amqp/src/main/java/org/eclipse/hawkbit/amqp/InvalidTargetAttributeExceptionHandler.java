/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.amqp;

import org.eclipse.hawkbit.repository.exception.InvalidTargetAttributeException;

/**
 * An error handler for all invalid target attributes resulting from AMQP.
 */
public class InvalidTargetAttributeExceptionHandler extends AbstractAmqpErrorHandler<InvalidTargetAttributeException> {

    @Override
    public Class<InvalidTargetAttributeException> getExceptionClass() {
        return InvalidTargetAttributeException.class;
    }
}