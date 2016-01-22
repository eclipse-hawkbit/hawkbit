/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.eventbus.event;

import org.eclipse.hawkbit.repository.model.RolloutGroup;

/**
 *
 */
public class RolloutGroupStatusUpdateEvent extends AbstractBaseEntityEvent<RolloutGroup> {
    private static final long serialVersionUID = 181780358321768629L;

    /**
     * @param rolloutGroup
     */
    public RolloutGroupStatusUpdateEvent(final RolloutGroup rolloutGroup) {
        super(rolloutGroup);
    }

}
