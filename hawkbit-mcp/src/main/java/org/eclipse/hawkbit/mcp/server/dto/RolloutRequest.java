/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mcp.server.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutRestRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutRestRequestBodyPut;

/**
 * Sealed interface for rollout management operations including CRUD and lifecycle.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RolloutRequest.Create.class, name = "Create"),
        @JsonSubTypes.Type(value = RolloutRequest.Update.class, name = "Update"),
        @JsonSubTypes.Type(value = RolloutRequest.Delete.class, name = "Delete"),
        @JsonSubTypes.Type(value = RolloutRequest.Start.class, name = "Start"),
        @JsonSubTypes.Type(value = RolloutRequest.Pause.class, name = "Pause"),
        @JsonSubTypes.Type(value = RolloutRequest.Stop.class, name = "Stop"),
        @JsonSubTypes.Type(value = RolloutRequest.Resume.class, name = "Resume"),
        @JsonSubTypes.Type(value = RolloutRequest.Approve.class, name = "Approve"),
        @JsonSubTypes.Type(value = RolloutRequest.Deny.class, name = "Deny"),
        @JsonSubTypes.Type(value = RolloutRequest.Retry.class, name = "Retry"),
        @JsonSubTypes.Type(value = RolloutRequest.TriggerNextGroup.class, name = "TriggerNextGroup")
})
public sealed interface RolloutRequest
        permits RolloutRequest.Create, RolloutRequest.Update, RolloutRequest.Delete,
                RolloutRequest.Start, RolloutRequest.Pause, RolloutRequest.Stop,
                RolloutRequest.Resume, RolloutRequest.Approve, RolloutRequest.Deny,
                RolloutRequest.Retry, RolloutRequest.TriggerNextGroup {

    /**
     * Request to create a new rollout.
     *
     * @param body the request body containing rollout data
     */
    record Create(MgmtRolloutRestRequestBodyPost body) implements RolloutRequest {}

    /**
     * Request to update an existing rollout.
     *
     * @param rolloutId the rollout ID
     * @param body      the request body containing updated rollout data
     */
    record Update(Long rolloutId, MgmtRolloutRestRequestBodyPut body) implements RolloutRequest {}

    /**
     * Request to delete a rollout.
     *
     * @param rolloutId the rollout ID to delete
     */
    record Delete(Long rolloutId) implements RolloutRequest {}

    /**
     * Request to start a rollout.
     *
     * @param rolloutId the rollout ID to start
     */
    record Start(Long rolloutId) implements RolloutRequest {}

    /**
     * Request to pause a rollout.
     *
     * @param rolloutId the rollout ID to pause
     */
    record Pause(Long rolloutId) implements RolloutRequest {}

    /**
     * Request to stop a rollout.
     *
     * @param rolloutId the rollout ID to stop
     */
    record Stop(Long rolloutId) implements RolloutRequest {}

    /**
     * Request to resume a paused rollout.
     *
     * @param rolloutId the rollout ID to resume
     */
    record Resume(Long rolloutId) implements RolloutRequest {}

    /**
     * Request to approve a rollout.
     *
     * @param rolloutId the rollout ID to approve
     * @param remark    optional remark for the approval
     */
    record Approve(Long rolloutId, String remark) implements RolloutRequest {}

    /**
     * Request to deny a rollout.
     *
     * @param rolloutId the rollout ID to deny
     * @param remark    optional remark for the denial
     */
    record Deny(Long rolloutId, String remark) implements RolloutRequest {}

    /**
     * Request to retry a rollout.
     *
     * @param rolloutId the rollout ID to retry
     */
    record Retry(Long rolloutId) implements RolloutRequest {}

    /**
     * Request to trigger the next group in a rollout.
     *
     * @param rolloutId the rollout ID
     */
    record TriggerNextGroup(Long rolloutId) implements RolloutRequest {}
}
