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

import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Unit Tests - Repository")
@Stories("Deployment Management")
public class ActionTest {

    @Test
    @Description("Ensures that timeforced moded switch from soft to forces after defined timeframe.")
    public void timeforcedHitNewHasCodeIsGenerated() throws InterruptedException {

        // current time + 1 seconds
        final long sleepTime = 1000;
        final long timeForceTimeAt = System.currentTimeMillis() + sleepTime;
        final JpaAction timeforcedAction = new JpaAction();
        timeforcedAction.setActionType(ActionType.TIMEFORCED);
        timeforcedAction.setForcedTime(timeForceTimeAt);
        assertThat(timeforcedAction.isForce()).isFalse();

        // wait until timeforce time is hit
        Thread.sleep(sleepTime + 100);
        assertThat(timeforcedAction.isForce()).isTrue();
    }
}
