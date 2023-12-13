/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.builder;

/**
 * Create and update builder DTO.
 *
 * @param <T>
 *            update or create builder interface
 */
public abstract class AbstractSoftwareModuleTypeUpdateCreate<T> extends AbstractTypeUpdateCreate<T> {

    protected int maxAssignments = 1;

    public T maxAssignments(final int maxAssignments) {
        this.maxAssignments = maxAssignments;
        return (T) this;
    }

    public int getMaxAssignments() {
        return maxAssignments;
    }
}
