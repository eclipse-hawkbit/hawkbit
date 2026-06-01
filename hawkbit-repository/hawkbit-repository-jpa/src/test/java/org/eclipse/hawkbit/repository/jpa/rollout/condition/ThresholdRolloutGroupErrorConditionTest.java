/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rollout.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ThresholdRolloutGroupErrorConditionTest {

    private static final long ROLLOUT_ID = 1L;
    private static final long GROUP_ID = 10L;
    private static final List<Action.Status> ERROR_STATUSES = List.of(Action.Status.ERROR, Action.Status.CANCELED);

    @Mock
    private ActionRepository actionRepository;
    @Mock
    private Rollout rollout;
    @Mock
    private RolloutGroup rolloutGroup;

    private ThresholdRolloutGroupErrorCondition condition;

    @BeforeEach
    void setUp() {
        condition = new ThresholdRolloutGroupErrorCondition(actionRepository);
        when(rollout.getId()).thenReturn(ROLLOUT_ID);
        when(rolloutGroup.getId()).thenReturn(GROUP_ID);
        when(rolloutGroup.getTotalTargets()).thenReturn(5);
    }

    @Test
    void errorStatusExceedsThreshold() {
        // 2 ERROR / 5 = 40% > 10%
        when(actionRepository.countByRolloutIdAndRolloutGroupIdAndStatusIn(ROLLOUT_ID, GROUP_ID, ERROR_STATUSES)).thenReturn(2L);
        assertThat(condition.eval(rollout, rolloutGroup, "10")).isTrue();
    }

    @Test
    void canceledStatusExceedsThreshold() {
        // Bug 3: 1 CANCELED / 5 = 20% > 10% must fire
        when(actionRepository.countByRolloutIdAndRolloutGroupIdAndStatusIn(ROLLOUT_ID, GROUP_ID, ERROR_STATUSES)).thenReturn(1L);
        assertThat(condition.eval(rollout, rolloutGroup, "10")).isTrue();
    }

    @Test
    void errorAndCanceledCombinedExceedThreshold() {
        // 3 combined (ERROR+CANCELED) / 5 = 60% > 50%
        when(actionRepository.countByRolloutIdAndRolloutGroupIdAndStatusIn(ROLLOUT_ID, GROUP_ID, ERROR_STATUSES)).thenReturn(3L);
        assertThat(condition.eval(rollout, rolloutGroup, "50")).isTrue();
    }

    @Test
    void canceledStatusBelowThreshold() {
        // 1 CANCELED / 5 = 20% < 50%
        when(actionRepository.countByRolloutIdAndRolloutGroupIdAndStatusIn(ROLLOUT_ID, GROUP_ID, ERROR_STATUSES)).thenReturn(1L);
        assertThat(condition.eval(rollout, rolloutGroup, "50")).isFalse();
    }

    @Test
    void noErrorsOrCanceledDoesNotFire() {
        when(actionRepository.countByRolloutIdAndRolloutGroupIdAndStatusIn(ROLLOUT_ID, GROUP_ID, ERROR_STATUSES)).thenReturn(0L);
        assertThat(condition.eval(rollout, rolloutGroup, "10")).isFalse();
    }

    @Test
    void exactlyAtThresholdDoesNotFire() {
        // strict >: 1/5 = 20%, threshold=20% → not exceeded
        when(actionRepository.countByRolloutIdAndRolloutGroupIdAndStatusIn(ROLLOUT_ID, GROUP_ID, ERROR_STATUSES)).thenReturn(1L);
        assertThat(condition.eval(rollout, rolloutGroup, "20")).isFalse();
    }

    @Test
    void zeroTotalTargetsDoesNotFire() {
        when(rolloutGroup.getTotalTargets()).thenReturn(0);
        assertThat(condition.eval(rollout, rolloutGroup, "10")).isFalse();
    }

    @Test
    void invalidThresholdExpressionDoesNotFire() {
        when(actionRepository.countByRolloutIdAndRolloutGroupIdAndStatusIn(ROLLOUT_ID, GROUP_ID, ERROR_STATUSES)).thenReturn(1L);
        assertThat(condition.eval(rollout, rolloutGroup, "notANumber")).isFalse();
    }
}
