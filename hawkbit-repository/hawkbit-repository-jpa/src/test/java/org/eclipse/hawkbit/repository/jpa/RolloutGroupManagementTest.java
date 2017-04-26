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

import org.eclipse.hawkbit.repository.event.remote.RolloutDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("Rollout Management")
public class RolloutGroupManagementTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Verifies that management get access reacts as specfied on calls for non existing entities by means "
            + "of Optional not present.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void nonExistingEntityAccessReturnsNotPresent() {
        assertThat(rolloutGroupManagement.findRolloutGroupById(NOT_EXIST_IDL)).isNotPresent();
        assertThat(rolloutGroupManagement.findRolloutGroupWithDetailedStatus(NOT_EXIST_IDL)).isNotPresent();

    }

    @Test
    @Description("Verifies that management queries react as specfied on calls for non existing entities "
            + " by means of throwing EntityNotFoundException.")
    @ExpectEvents({ @Expect(type = RolloutDeletedEvent.class, count = 0),
            @Expect(type = RolloutGroupCreatedEvent.class, count = 10),
            @Expect(type = RolloutGroupUpdatedEvent.class, count = 10),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = RolloutUpdatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 10) })
    public void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        testdataFactory.createRollout("xxx");

        verifyThrownExceptionBy(() -> rolloutGroupManagement.countRolloutGroupsByRolloutId(NOT_EXIST_IDL), "Rollout");
        verifyThrownExceptionBy(() -> rolloutGroupManagement.countTargetsOfRolloutsGroup(NOT_EXIST_IDL),
                "RolloutGroup");
        verifyThrownExceptionBy(
                () -> rolloutGroupManagement.findAllRolloutGroupsWithDetailedStatus(NOT_EXIST_IDL, pageReq), "Rollout");
        verifyThrownExceptionBy(() -> rolloutGroupManagement.findAllTargetsWithActionStatus(pageReq, NOT_EXIST_IDL),
                "RolloutGroup");
        verifyThrownExceptionBy(() -> rolloutGroupManagement.findRolloutGroupsAll(NOT_EXIST_IDL, "name==*", pageReq),
                "Rollout");

        verifyThrownExceptionBy(() -> rolloutGroupManagement.findRolloutGroupTargets(NOT_EXIST_IDL, pageReq),
                "RolloutGroup");
        verifyThrownExceptionBy(() -> rolloutGroupManagement.findRolloutGroupTargets(NOT_EXIST_IDL, "name==*", pageReq),
                "RolloutGroup");
    }

}
