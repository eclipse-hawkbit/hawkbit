/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.builder.TargetBuilder;
import org.eclipse.hawkbit.repository.builder.TargetCreate;
import org.eclipse.hawkbit.repository.builder.TargetUpdate;
import org.eclipse.hawkbit.repository.model.Target;

/**
 * Builder implementation for {@link Target}.
 *
 */
public class JpaTargetBuilder implements TargetBuilder {
    final private TargetTypeManagement targetTypeManagement;

    /**
     * @param targetTypeManagement
     *          Target type management
     */
    public JpaTargetBuilder(TargetTypeManagement targetTypeManagement) {
        this.targetTypeManagement = targetTypeManagement;
    }

    @Override
    public TargetUpdate update(final String controllerId) {
        return new JpaTargetUpdate(controllerId);
    }

    @Override
    public TargetCreate create() {
        return new JpaTargetCreate(targetTypeManagement);
    }

}
