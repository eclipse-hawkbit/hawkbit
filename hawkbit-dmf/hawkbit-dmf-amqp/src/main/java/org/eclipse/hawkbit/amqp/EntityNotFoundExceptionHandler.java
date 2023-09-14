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

import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;

/**
 * An error handler for entity not found exception resulting from AMQP.
 */
public class EntityNotFoundExceptionHandler extends AbstractAmqpErrorHandler<EntityNotFoundException> {

    @Override
    public Class<EntityNotFoundException> getExceptionClass() {
        return EntityNotFoundException.class;
    }
}
