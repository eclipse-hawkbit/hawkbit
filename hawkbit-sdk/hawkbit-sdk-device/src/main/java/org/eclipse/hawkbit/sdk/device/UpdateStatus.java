/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk.device;

import org.eclipse.hawkbit.ddi.json.model.DdiActionFeedback;
import org.eclipse.hawkbit.ddi.json.model.DdiResult;
import org.eclipse.hawkbit.ddi.json.model.DdiStatus;

import java.util.List;

record UpdateStatus(Status status, List<String> messages) {

    /**
     * The status to response to the hawkBit update server if an simulated update process should be respond with
     * successful or failure update.
     */
    enum Status {

        /**
         * Update has been successful and response the successful update.
         */
        SUCCESSFUL(DdiStatus.ExecutionStatus.CLOSED, DdiResult.FinalResult.SUCCESS, 200),

        /**
         * Update has been not successful and response the error update.
         */
        ERROR(DdiStatus.ExecutionStatus.CLOSED, DdiResult.FinalResult.FAILURE, null),

        /**
         * Update is running (intermediate status).
         */
        RUNNING(DdiStatus.ExecutionStatus.PROCEEDING, DdiResult.FinalResult.NONE, null),

        /**
         * Device starts to download.
         */
        DOWNLOADING(DdiStatus.ExecutionStatus.DOWNLOAD, DdiResult.FinalResult.NONE, null),

        /**
         * Device is finished with downloading.
         */
        DOWNLOADED(DdiStatus.ExecutionStatus.DOWNLOADED, DdiResult.FinalResult.NONE, null);

        private final DdiStatus.ExecutionStatus executionStatus;
        private final DdiResult.FinalResult finalResult;
        private final Integer code;

        Status(final DdiStatus.ExecutionStatus executionStatus, final DdiResult.FinalResult finalResult,
                final Integer code) {
            this.executionStatus = executionStatus;
            this.finalResult = finalResult;
            this.code = code;
        }
    }

    DdiActionFeedback feedback() {
        return new DdiActionFeedback(null,
                new DdiStatus(status.executionStatus, new DdiResult(status.finalResult, null), status.code, messages));
    }
}
