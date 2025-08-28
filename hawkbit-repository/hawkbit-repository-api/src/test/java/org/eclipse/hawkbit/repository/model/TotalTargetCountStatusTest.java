/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Feature: Component Tests - TotalTargetCountStatus<br/>
 * Story: TotalTargetCountStatus should correctly present finished DOWNLOAD_ONLY actions
 */
class TotalTargetCountStatusTest {

    private final List<TotalTargetCountActionStatus> targetCountActionStatuses = Arrays.asList(
            new TotalTargetCountActionStatus(Action.Status.SCHEDULED, 1L),
            new TotalTargetCountActionStatus(Action.Status.ERROR, 2L),
            new TotalTargetCountActionStatus(Action.Status.FINISHED, 3L),
            new TotalTargetCountActionStatus(Action.Status.CANCELED, 4L),
            new TotalTargetCountActionStatus(Action.Status.RETRIEVED, 5L),
            new TotalTargetCountActionStatus(Action.Status.RUNNING, 6L),
            new TotalTargetCountActionStatus(Action.Status.WARNING, 7L),
            new TotalTargetCountActionStatus(Action.Status.DOWNLOAD, 8L),
            new TotalTargetCountActionStatus(Action.Status.CANCELING, 9L),
            new TotalTargetCountActionStatus(Action.Status.DOWNLOADED, 10L));

    /**
     * Different Action Statuses should be correctly mapped to the corresponding TotalTargetCountStatus.Status
     */
    @Test
    void shouldCorrectlyMapActionStatuses() {
        TotalTargetCountStatus status = new TotalTargetCountStatus(targetCountActionStatuses, 55L,
                Action.ActionType.FORCED);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.SCHEDULED)).isEqualTo(1L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.ERROR)).isEqualTo(2L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.FINISHED)).isEqualTo(3L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.CANCELLED)).isEqualTo(4L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.RUNNING)).isEqualTo(45L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.NOTSTARTED)).isZero();
        assertThat(status.getFinishedPercent()).isEqualTo((float) 100 * 3 / 55);
    }

    /**
     * When an empty list is passed to the TotalTargetCountStatus, all actions should be displayed as 
     * NOTSTARTED
     */
    @Test
    void shouldCorrectlyMapActionStatusesToNotStarted() {
        TotalTargetCountStatus status = new TotalTargetCountStatus(Collections.emptyList(), 55L,
                Action.ActionType.FORCED);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.SCHEDULED)).isZero();
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.ERROR)).isZero();
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.FINISHED)).isZero();
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.CANCELLED)).isZero();
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.RUNNING)).isZero();
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.NOTSTARTED)).isEqualTo(55L);
        assertThat(status.getFinishedPercent()).isZero();
    }

    /**
     * DownloadOnly actions should be displayed as FINISHED when they have ActionStatus#DOWNLOADED
     */
    @Test
    void shouldCorrectlyMapActionStatusesInDownloadOnlyCase() {
        TotalTargetCountStatus status = new TotalTargetCountStatus(targetCountActionStatuses, 55L,
                Action.ActionType.DOWNLOAD_ONLY);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.SCHEDULED)).isEqualTo(1L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.ERROR)).isEqualTo(2L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.FINISHED)).isEqualTo(13L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.CANCELLED)).isEqualTo(4L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.RUNNING)).isEqualTo(35L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.NOTSTARTED)).isZero();
        assertThat(status.getFinishedPercent()).isEqualTo((float) 100 * 13 / 55);
    }
}
