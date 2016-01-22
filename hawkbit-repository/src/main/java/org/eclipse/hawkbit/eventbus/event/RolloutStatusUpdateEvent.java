/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
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
