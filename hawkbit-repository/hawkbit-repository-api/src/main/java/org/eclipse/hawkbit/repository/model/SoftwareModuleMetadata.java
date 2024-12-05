/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

/**
 * {@link MetaData} element of a {@link SoftwareModule}.
 */
public interface SoftwareModuleMetadata extends MetaData {

    /**
     * @return {@link SoftwareModule} this entry belongs to.
     */
    SoftwareModule getSoftwareModule();

    @Override
    default Long getEntityId() {
        return getSoftwareModule().getId();
    }

    /**
     * @return <code>true</code> if element is visible for targets as part of
     *         {@link Action}.
     */
    boolean isTargetVisible();
}
