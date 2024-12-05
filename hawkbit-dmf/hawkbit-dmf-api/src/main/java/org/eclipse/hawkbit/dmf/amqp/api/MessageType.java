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

/**
 * The amqp message types which can be handled.
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
     * The request to delete a target.
     */
    THING_REMOVED,
    /**
     * DMF receiver health check type.
     */
    PING,
    /**
     * DMF receiver health check response type.
     */
    PING_RESPONSE
}