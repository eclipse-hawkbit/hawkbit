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
 * {@link SoftwareModuleType} is an abstract definition used in
 * {@link DistributionSetType}s and includes additional {@link SoftwareModule}
 * specific information.
 *
 */
public interface SoftwareModuleType extends Type {

    /**
     * @return maximum assignments of an {@link SoftwareModule} of this type to
     *         a {@link DistributionSet}.
     */
    int getMaxAssignments();
}
