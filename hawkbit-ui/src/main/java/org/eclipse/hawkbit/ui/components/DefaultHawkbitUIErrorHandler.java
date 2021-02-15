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
import com.vaadin.server.UploadException;

/**
 * Default handler for the Hawkbit UI.
 */
public class DefaultHawkbitUIErrorHandler extends AbstractUIErrorHandler {

    private static final long serialVersionUID = 1L;

    @Override
    public void error(final ErrorEvent event) {

        // filter upload exceptions
        if (event.getThrowable() instanceof UploadException) {
            return;
        }

        showNotification(event);
    }

}
