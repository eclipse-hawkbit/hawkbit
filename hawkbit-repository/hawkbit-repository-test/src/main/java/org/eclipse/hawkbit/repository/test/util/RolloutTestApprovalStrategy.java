/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.test.util;

import org.eclipse.hawkbit.repository.RolloutApprovalStrategy;
import org.eclipse.hawkbit.repository.model.Rollout;

/**
 * Provides an approval strategy that can be manipulated by setting the {link
 * {@link #approvalNeeded}} flag used for testing.
 */
public class RolloutTestApprovalStrategy implements RolloutApprovalStrategy {

    private boolean approvalNeeded = false;

    @Override
    public boolean isApprovalNeeded(Rollout rollout) {
        return approvalNeeded;
    }

    public void setApprovalNeeded(boolean approvalNeeded) {
        this.approvalNeeded = approvalNeeded;
    }

    @Override
    public void onApprovalRequired(Rollout rollout) {
        // do nothing, as no action is needed when testing
    }

    @Override
    public String getApprovalUser(Rollout rollout) {
        return null;
    }
}
