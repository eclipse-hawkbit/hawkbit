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

import org.eclipse.hawkbit.repository.model.RolloutGroup;

/**
 * Defines the {@link AbstractPropertyChangeEvent} of {@link RolloutGroup}.
 */
public class RolloutGroupPropertyChangeEvent extends AbstractPropertyChangeEvent<RolloutGroup> {

    private static final long serialVersionUID = 4026477044419472686L;

    /**
     * 
     * @param rolloutGroup
     * @param changeSetValues
     */
    public RolloutGroupPropertyChangeEvent(final RolloutGroup rolloutGroup,
            final Map<String, AbstractPropertyChangeEvent<RolloutGroup>.Values> changeSetValues) {
        super(rolloutGroup, changeSetValues);
    }

}
