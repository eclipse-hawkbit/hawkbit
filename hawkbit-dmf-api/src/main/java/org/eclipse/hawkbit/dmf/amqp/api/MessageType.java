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
 *
 *
 */
public enum MessageType {

    /**
     * The event type.
     */
    EVENT,

    /**
     * the thing created type.
     */
    THING_CREATED,

    /**
     * The authentication type.
     */
    AUTHENTIFICATION,

}
