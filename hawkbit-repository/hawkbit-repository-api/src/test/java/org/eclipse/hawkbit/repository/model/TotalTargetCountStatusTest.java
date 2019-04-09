/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class TotalTargetCountStatusTest {

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
            new TotalTargetCountActionStatus(Action.Status.DOWNLOADED, 10L)
    );

    @Test
    public void shouldCorrectlyMapActionStatuses(){
        TotalTargetCountStatus status = new TotalTargetCountStatus(targetCountActionStatuses, 55L, Action.ActionType.FORCED);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.SCHEDULED)).isEqualTo(1L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.ERROR)).isEqualTo(2L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.FINISHED)).isEqualTo(3L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.CANCELLED)).isEqualTo(4L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.RUNNING)).isEqualTo(45L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.NOTSTARTED)).isEqualTo(0L);
        assertThat(status.getFinishedPercent()).isEqualTo((float) 100 * 3/55);
    }

    @Test
    public void shouldCorrectlyMapActionStatusesToNotStarted(){
        TotalTargetCountStatus status = new TotalTargetCountStatus(Collections.emptyList(), 55L, Action.ActionType.FORCED);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.SCHEDULED)).isEqualTo(0L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.ERROR)).isEqualTo(0L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.FINISHED)).isEqualTo(0L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.CANCELLED)).isEqualTo(0L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.RUNNING)).isEqualTo(0L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.NOTSTARTED)).isEqualTo(55L);
        assertThat(status.getFinishedPercent()).isEqualTo(0);
    }

    @Test
    public void shouldCorrectlyMapActionStatusesInDownloadOnlyCase(){
        TotalTargetCountStatus status = new TotalTargetCountStatus(targetCountActionStatuses, 55L, Action.ActionType.DOWNLOAD_ONLY);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.SCHEDULED)).isEqualTo(1L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.ERROR)).isEqualTo(2L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.FINISHED)).isEqualTo(13L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.CANCELLED)).isEqualTo(4L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.RUNNING)).isEqualTo(35L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.NOTSTARTED)).isEqualTo(0L);
        assertThat(status.getFinishedPercent()).isEqualTo((float) 100 * 13/55);
    }
}
