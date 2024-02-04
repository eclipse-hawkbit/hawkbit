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
 * The constant of all amqp message header.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageHeaderKey {

    /**
     * The message type.
     */
    public static final String TYPE = "type";
    /**
     * The used tenant.
     */
    public static final String TENANT = "tenant";
    /**
     * The name of the thing/target.
     */
    public static final String THING_ID = "thingId";
    /**
     * The name of the sender who has send the message.
     */
    public static final String SENDER = "sender";
    /**
     * The topic to handle events different.
     */
    public static final String TOPIC = "topic";
    /**
     * The content type.
     */
    public static final String CONTENT_TYPE = "content-type";
}