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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.eclipse.hawkbit.repository.event.remote.RolloutDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.jupiter.api.Test;

/**
 * Feature: Component Tests - Repository<br/>
 * Story: Rollout Management
 */
class RolloutGroupManagementTest extends AbstractJpaIntegrationTest {

    /**
     * Verifies that management get access reacts as specified on calls for non existing entities by means 
     * of Optional not present.
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    void nonExistingEntityAccessReturnsNotPresent() {
        assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> rolloutGroupManagement.get(NOT_EXIST_IDL));
        assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> rolloutGroupManagement.getWithDetailedStatus(NOT_EXIST_IDL));
    }

    /**
     * Verifies that management queries react as specified on calls for non existing entities 
     *  by means of throwing EntityNotFoundException.
     */
    @Test
    @ExpectEvents({
            @Expect(type = RolloutCreatedEvent.class, count = 1),
            @Expect(type = RolloutUpdatedEvent.class, count = 1),
            @Expect(type = RolloutGroupCreatedEvent.class, count = 5),
            @Expect(type = RolloutGroupUpdatedEvent.class, count = 5),
            @Expect(type = RolloutDeletedEvent.class, count = 0),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = TargetCreatedEvent.class, count = 125) })
    void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        testdataFactory.createRollout();

        verifyThrownExceptionBy(() -> rolloutGroupManagement.countTargetsOfRolloutsGroup(NOT_EXIST_IDL), "RolloutGroup");
        verifyThrownExceptionBy(() -> rolloutGroupManagement.findByRolloutWithDetailedStatus(NOT_EXIST_IDL, PAGE), "Rollout");
        verifyThrownExceptionBy(() -> rolloutGroupManagement.findByRolloutAndRsql(NOT_EXIST_IDL, "name==*", PAGE), "Rollout");

        verifyThrownExceptionBy(() -> rolloutGroupManagement.findTargetsOfRolloutGroup(NOT_EXIST_IDL, PAGE), "RolloutGroup");
        verifyThrownExceptionBy(() -> rolloutGroupManagement.findTargetsOfRolloutGroupByRsql(NOT_EXIST_IDL, "name==*", PAGE), "RolloutGroup");
    }

    /**
     * Tests the rollout group status mapping.
     */
    @Test
    void testRolloutGroupStatusConvert() {
        final long id = rolloutGroupRepository.findByRolloutId(
                        testdataFactory.createAndStartRollout(1, 0, 1, "100", "80").getId(), PAGE).getContent()
                .get(0).getId();
        for (final RolloutGroup.RolloutGroupStatus status : RolloutGroup.RolloutGroupStatus.values()) {
            final JpaRolloutGroup rolloutGroup = (JpaRolloutGroup) rolloutGroupManagement.get(id);
            rolloutGroup.setStatus(status);
            rolloutGroupRepository.save(rolloutGroup);
            assertThat(rolloutGroupManagement.get(id).getStatus()).isEqualTo(status);
        }
    }
}