/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.action;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * A json annotated rest model for ActionStatus to RESTful API representation.
 */
@Data
@Accessors(chain = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtActionStatus {

    @Schema(example = "21")
    private Long id;

    @Schema(example = "running")
    private Type type;

    private List<String> messages;

    @Schema(example = "1691065929524")
    private Long reportedAt;

    @Schema(example = "1691065929524")
    private Long timestamp;

    @Schema(example = "200")
    private Integer code;

    public enum Type {

        /**
         * Action is finished successfully for this target.
         */
        FINISHED,

        /**
         * Action has failed for this target.
         */
        ERROR,

        /**
         * Action is still running but with warnings.
         */
        WARNING,

        /**
         * Action is still running for this target.
         */
        RUNNING,

        /**
         * Action has been canceled for this target.
         */
        CANCELED,

        /**
         * Action is in canceling state and waiting for controller confirmation.
         */
        CANCELING,

        /**
         * Action has been send to the target.
         */
        RETRIEVED,

        /**
         * Action requests download by this target which has now started.
         */
        DOWNLOAD,

        /**
         * Action is in waiting state, e.g. the action is scheduled in a rollout
         * but not yet activated.
         */
        SCHEDULED,

        /**
         * Cancellation has been rejected by the controller.
         */
        CANCEL_REJECTED,

        /**
         * Action has been downloaded by the target and waiting for update to
         * start.
         */
        DOWNLOADED,

        /**
         * Action is waiting to be confirmed by the user
         */
        WAIT_FOR_CONFIRMATION;

        @JsonValue
        public String getName() {
            return this.name().toLowerCase();
        }

        @JsonCreator
        public static Type forValue(String s) {
            return Type.valueOf(s.toUpperCase());
        }
    }
}