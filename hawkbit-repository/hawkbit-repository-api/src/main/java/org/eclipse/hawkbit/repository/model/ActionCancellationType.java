/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

/**
 * Defines if and how actions should be canceled when :
 *      - invalidating a distribution set
 *      - stopping a rollout
 *      - deleting a rollout
 */
public enum ActionCancellationType {
    /**
     * will perform a FORCE action cancellation - will put them in CANCELED state.
     */
    FORCE,

    /**
     * will perform a SOFT action cancellation - will put them in CANCELING state.
     */
    SOFT,

    /**
     * Used in distribution set invalidation - will ONLY invalidate the DS, will not change action status
     */
    NONE
}
