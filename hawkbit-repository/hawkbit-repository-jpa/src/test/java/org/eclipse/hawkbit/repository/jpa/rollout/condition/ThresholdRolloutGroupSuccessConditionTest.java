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

import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class ThresholdRolloutGroupSuccessConditionTest {

    private static final long ROLLOUT_ID = 1L;
    private static final long GROUP_ID = 10L;

    @Mock
    private ActionRepository actionRepository;
    @Mock
    private Rollout rollout;
    @Mock
    private RolloutGroup rolloutGroup;

    private ThresholdRolloutGroupSuccessCondition condition;

    @BeforeEach
    void setUp() {
        condition = new ThresholdRolloutGroupSuccessCondition(actionRepository);
        when(rollout.getId()).thenReturn(ROLLOUT_ID);
        when(rolloutGroup.getId()).thenReturn(GROUP_ID);
        when(rolloutGroup.getTotalTargets()).thenReturn(5);
    }

    @Test
    void finishedRatioMeetsThreshold() {
        // 4 FINISHED / 5 = 80% >= 80%
        when(actionRepository.countByRolloutIdAndRolloutGroupIdAndStatus(ROLLOUT_ID, GROUP_ID, Action.Status.FINISHED)).thenReturn(4L);
        assertThat(condition.eval(rollout, rolloutGroup, "80")).isTrue();
    }

    @Test
    void finishedRatioBelowThreshold() {
        // 3 FINISHED / 5 = 60% < 80%
        when(actionRepository.countByRolloutIdAndRolloutGroupIdAndStatus(ROLLOUT_ID, GROUP_ID, Action.Status.FINISHED)).thenReturn(3L);
        assertThat(condition.eval(rollout, rolloutGroup, "80")).isFalse();
    }

    @Test
    void exactlyAtThresholdFires() {
        // uses >=: 1/5 = 20%, threshold=20% → fires (contrast: error uses >)
        when(actionRepository.countByRolloutIdAndRolloutGroupIdAndStatus(ROLLOUT_ID, GROUP_ID, Action.Status.FINISHED)).thenReturn(1L);
        assertThat(condition.eval(rollout, rolloutGroup, "20")).isTrue();
    }

    @Test
    void zeroTotalTargetsReturnsTrue() {
        // opposite of error condition: no targets = group considered done
        when(rolloutGroup.getTotalTargets()).thenReturn(0);
        assertThat(condition.eval(rollout, rolloutGroup, "100")).isTrue();
    }

    @Test
    void downloadOnlyUsesDownloadedStatus() {
        when(rollout.getActionType()).thenReturn(Action.ActionType.DOWNLOAD_ONLY);
        when(actionRepository.countByRolloutIdAndRolloutGroupIdAndStatus(ROLLOUT_ID, GROUP_ID, Action.Status.DOWNLOADED)).thenReturn(5L);
        assertThat(condition.eval(rollout, rolloutGroup, "100")).isTrue();
    }

    @Test
    void downloadOnlyDoesNotCountFinishedStatus() {
        when(rollout.getActionType()).thenReturn(Action.ActionType.DOWNLOAD_ONLY);
        when(actionRepository.countByRolloutIdAndRolloutGroupIdAndStatus(ROLLOUT_ID, GROUP_ID, Action.Status.DOWNLOADED)).thenReturn(0L);
        assertThat(condition.eval(rollout, rolloutGroup, "100")).isFalse();
    }

    @Test
    void noFinishedActionsDoesNotFire() {
        when(actionRepository.countByRolloutIdAndRolloutGroupIdAndStatus(ROLLOUT_ID, GROUP_ID, Action.Status.FINISHED)).thenReturn(0L);
        assertThat(condition.eval(rollout, rolloutGroup, "10")).isFalse();
    }

    @Test
    void invalidThresholdExpressionReturnsFalse() {
        when(actionRepository.countByRolloutIdAndRolloutGroupIdAndStatus(ROLLOUT_ID, GROUP_ID, Action.Status.FINISHED)).thenReturn(5L);
        assertThat(condition.eval(rollout, rolloutGroup, "notANumber")).isFalse();
    }
}
