/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.test.util;

import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.hawkbit.repository.RolloutApprovalStrategy;
import org.eclipse.hawkbit.repository.model.Rollout;

/**
 * Provides an approval strategy that can be manipulated by setting the  {@link #approvalNeeded}} flag used for testing.
 */
@Setter
@EqualsAndHashCode
@ToString
public class RolloutTestApprovalStrategy implements RolloutApprovalStrategy {

    private boolean approvalNeeded = false;
    private String approvalDecidedBy;

    @Override
    public boolean isApprovalNeeded(Rollout rollout) {
        return approvalNeeded;
    }

    @Override
    public String getApprovalUser(Rollout rollout) {
        return approvalDecidedBy;
    }

    @Override
    public void onApprovalRequired(Rollout rollout) {
        // do nothing, as no action is needed when testing
    }
}