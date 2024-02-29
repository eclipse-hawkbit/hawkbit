/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.dmf.amqp.api;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Global constants for RabbitMQ settings.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AmqpSettings {

    public static final String DMF_EXCHANGE = "dmf.exchange";
    public static final String AUTHENTICATION_EXCHANGE = "authentication.exchange";
}