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
 * The amqp message types which can be handled.
 *
 */
public enum MessageType {

    /**
     * The event type related to interaction with a thing.
     */
    EVENT,

    /**
     * The thing created type.
     */
    THING_CREATED,

    /**
     * The thing deleted type.
     */
    THING_DELETED,

    /**
     * DMF receiver health check type.
     */
    PING,

    /**
     * DMF receiver health check reponse type.
     */
    PING_RESPONSE;

}
