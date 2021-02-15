/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.exception;

/**
 * An handler that will handle a specific event if it can. Otherwise it
 * delegates it further in the chain.
 */
@FunctionalInterface
public interface ConditionalErrorHandler<T> {

    /**
     * Handle the given error if possible, otherwise delegate it to the
     * {@link EventHandlerChain}
     *
     * @param event
     *            the event
     * @param chain
     *            the EventHandlerChain
     */
    void handle(T event, EventHandlerChain<T> chain);

}
