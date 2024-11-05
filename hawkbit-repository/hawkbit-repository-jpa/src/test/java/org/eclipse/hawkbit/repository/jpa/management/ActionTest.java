/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.awaitility.Awaitility;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.junit.jupiter.api.Test;

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
        Awaitility.await().atMost(Duration.ofSeconds(2)).pollInterval(Duration.ofMillis(100))
                .until(timeforcedAction::isForcedOrTimeForced);
    }
}
