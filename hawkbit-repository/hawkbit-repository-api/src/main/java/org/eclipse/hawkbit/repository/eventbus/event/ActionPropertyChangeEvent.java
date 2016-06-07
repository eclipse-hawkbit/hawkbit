/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.eventbus.event;

import java.util.Map;

import org.eclipse.hawkbit.repository.model.Action;

/**
 * Defines the {@link AbstractPropertyChangeEvent} of {@link Action}.
 */
public class ActionPropertyChangeEvent extends AbstractPropertyChangeEvent<Action> {
    private static final long serialVersionUID = 181780358321768629L;

    /**
     * @param action
     * @param changeSetValues
     */
    public ActionPropertyChangeEvent(final Action action,
            final Map<String, AbstractPropertyChangeEvent<Action>.Values> changeSetValues) {
        super(action, changeSetValues);
    }

}
