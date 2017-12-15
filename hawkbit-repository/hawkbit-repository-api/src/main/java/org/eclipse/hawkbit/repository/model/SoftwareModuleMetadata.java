/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

/**
 * {@link MetaData} element of a {@link SoftwareModule}.
 *
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
