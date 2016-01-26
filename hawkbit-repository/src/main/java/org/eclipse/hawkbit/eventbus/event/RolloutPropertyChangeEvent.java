/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.eventbus.event;

import java.util.Map;

import org.eclipse.hawkbit.repository.model.Rollout;

/**
 * Defines the {@link AbstractPropertyChangeEvent} of {@link Rollout}.
 */
public class RolloutPropertyChangeEvent extends AbstractPropertyChangeEvent<Rollout> {
    private static final long serialVersionUID = 181780358321768629L;

    /**
     * 
     * @param rollout
     * @param changeSetValues
     */
    public RolloutPropertyChangeEvent(final Rollout rollout,
            final Map<String, AbstractPropertyChangeEvent<Rollout>.Values> changeSetValues) {
        super(rollout, changeSetValues);
    }

}
