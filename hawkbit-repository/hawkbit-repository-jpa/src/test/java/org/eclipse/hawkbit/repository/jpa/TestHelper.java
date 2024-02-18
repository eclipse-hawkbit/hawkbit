/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestHelper {

    public static void implicitLock(final DistributionSet set) {
        ((JpaDistributionSet) set).setOptLockRevision(set.getOptLockRevision() + 1);
    }

    public static void implicitLock(final SoftwareModule module) {
        ((JpaSoftwareModule) module).setOptLockRevision(module.getOptLockRevision() + 1);
    }
}