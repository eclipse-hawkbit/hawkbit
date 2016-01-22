/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.eventbus.event;

import org.eclipse.hawkbit.repository.model.Rollout;

/**
 *
 */
public class RolloutStatusUpdateEvent extends AbstractBaseEntityEvent<Rollout> {
    private static final long serialVersionUID = 181780358321768629L;

    /**
     * @param rollout
     */
    public RolloutStatusUpdateEvent(final Rollout rollout) {
        super(rollout);
    }

}
