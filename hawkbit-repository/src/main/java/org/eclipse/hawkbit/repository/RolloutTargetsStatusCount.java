/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents item with status and total targets count for rollout and rollout
 * group.
 *
 */
public class RolloutTargetsStatusCount {
    public enum RolloutTargetStatus {
        READY, RUNNING, ERROR, FINISHED, CANCELLED, NOTSTARTED
    }

    final Map<RolloutTargetStatus, Long> statusCountDetails = new HashMap<RolloutTargetsStatusCount.RolloutTargetStatus, Long>();

    /**
     * @return the statusCountDetails
     */
    public Map<RolloutTargetStatus, Long> getStatusCountDetails() {
        return statusCountDetails;
    }

}
