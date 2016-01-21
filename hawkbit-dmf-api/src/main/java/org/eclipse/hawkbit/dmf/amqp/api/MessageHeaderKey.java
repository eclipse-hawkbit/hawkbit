/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.dmf.amqp.api;

/**
 * The constant of all amqp message header.
 *
 *
 *
 */
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

    private MessageHeaderKey() {

    }

}
