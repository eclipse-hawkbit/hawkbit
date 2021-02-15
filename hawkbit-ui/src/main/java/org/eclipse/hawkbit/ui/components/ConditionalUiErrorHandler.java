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
import org.eclipse.hawkbit.exception.ConditionalErrorHandler;

/**
 * Extends the {@link ConditionalErrorHandler} for {@link ErrorEvent} for UI
 * purpose.
 */
public abstract class ConditionalUiErrorHandler extends AbstractUIErrorHandler
        implements ConditionalErrorHandler<ErrorEvent> {

}
