/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Unit Tests - Repository")
@Story("Deployment Management")
public class ActionTest {

    @Test
    @Description("Ensures that timeforced moded switch from soft to forces after defined timeframe.")
    public void timeforcedHitNewHasCodeIsGenerated() {

        // current time + 1 seconds
        final long sleepTime = 1000;
        final long timeForceTimeAt = System.currentTimeMillis() + sleepTime;
        final JpaAction timeforcedAction = new JpaAction();
        timeforcedAction.setActionType(ActionType.TIMEFORCED);
        timeforcedAction.setForcedTime(timeForceTimeAt);
        assertThat(timeforcedAction.isForcedOrTimeForced()).isFalse();

        // wait until timeforce time is hit
        Awaitility.await().atMost(Duration.TWO_SECONDS).pollInterval(Duration.ONE_HUNDRED_MILLISECONDS)
                .until(timeforcedAction::isForcedOrTimeForced);
    }
}
