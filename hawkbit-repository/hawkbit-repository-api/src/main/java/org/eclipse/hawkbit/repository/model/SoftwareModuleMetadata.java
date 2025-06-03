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

import java.io.Serializable;

/**
 * Metadata element of a {@link SoftwareModule}. The software module metadata is not only (key, value) pair (like the metadata of
 * targets and distribution sets), but also contains the information if the metadata is visible for targets as part of {@link Action}.
 */
public interface SoftwareModuleMetadata extends Serializable {

    /**
     * @return the key
     */
    String getKey();

    /**
     * @return the value
     */
    String getValue();

    /**
     * @return <code>true</code> if element is visible for targets as part of {@link Action}.
     */
    boolean isTargetVisible();
}
