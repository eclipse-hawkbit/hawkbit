/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

/**
 * {@link MetaData} of a {@link Target}.
 *
 */
public interface TargetMetadata extends MetaData {

    /**
     * @return {@link Target} of this {@link MetaData} entry.
     */
    Target getTarget();

    @Override
    default Long getEntityId() {
        return getTarget().getId();
    }
}
