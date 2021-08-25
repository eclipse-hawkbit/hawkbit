/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
