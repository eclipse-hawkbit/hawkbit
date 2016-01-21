/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import static org.fest.assertions.api.Assertions.assertThat;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.junit.Test;

public class ActionTest {

    // issue MECS-670 timeforced update and eTAG calculation
    @Test
    public void timeforcedHitNewHasCodeIsGenerated() throws InterruptedException {

        final boolean active = true;
        // current time + 1 seconds
        final long sleepTime = 1000;
        final long timeForceTimeAt = System.currentTimeMillis() + sleepTime;
        final Action timeforcedAction = new Action();
        timeforcedAction.setActionType(ActionType.TIMEFORCED);
        timeforcedAction.setForcedTime(timeForceTimeAt);
        final int knownHashCode = timeforcedAction.hashCode();
        assertThat(timeforcedAction.isForce()).isFalse();

        // wait until timeforce time is hit
        Thread.sleep(sleepTime + 100);
        assertThat(timeforcedAction.isForce()).isTrue();
        assertThat(timeforcedAction.hashCode()).isNotEqualTo(knownHashCode);
    }
}
