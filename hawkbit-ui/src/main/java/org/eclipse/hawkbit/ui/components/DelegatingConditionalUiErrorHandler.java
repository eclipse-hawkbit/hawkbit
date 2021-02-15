/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.components;

import com.vaadin.server.ErrorEvent;
import com.vaadin.server.ErrorHandler;
import org.eclipse.hawkbit.exception.ConditionalErrorHandler;
import org.eclipse.hawkbit.exception.ErrorHandlerChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Delegates an error from type {@link ErrorEvent} to any matching
 * {@link ConditionalUiErrorHandler}. If no handler can deal with the error, the
 * default {@link DefaultHawkbitUIErrorHandler} is used.
 */
public class DelegatingConditionalUiErrorHandler implements ErrorHandler {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(DelegatingConditionalUiErrorHandler.class);

    private final transient List<ConditionalErrorHandler<ErrorEvent>> errorHandlers;
    private final DefaultHawkbitUIErrorHandler defaultErrorHandler;

    /**
     * Constructor
     * 
     * @param conditionalErrorHandler
     *            all existing conditional based error handler
     */
    public DelegatingConditionalUiErrorHandler(final List<ConditionalUiErrorHandler> conditionalErrorHandler) {
        this.errorHandlers = new ArrayList<>(conditionalErrorHandler);
        this.defaultErrorHandler = new DefaultHawkbitUIErrorHandler();
    }

    @Override
    public void error(final ErrorEvent event) {
        ErrorHandlerChain.getHandler(errorHandlers, () -> {
            LOGGER.debug("No suitable UI error handler found - will use default one.");
            defaultErrorHandler.error(event);
            return null;
        }).doHandle(event);
    }

}
