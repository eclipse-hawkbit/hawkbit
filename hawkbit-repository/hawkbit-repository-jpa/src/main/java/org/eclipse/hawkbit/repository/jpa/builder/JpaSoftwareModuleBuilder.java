/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.builder.GenericSoftwareModuleUpdate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleBuilder;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleUpdate;
import org.eclipse.hawkbit.repository.model.SoftwareModule;

/**
 * Builder implementation for {@link SoftwareModule}.
 *
 */
public class JpaSoftwareModuleBuilder implements SoftwareModuleBuilder {

    private final SoftwareModuleTypeManagement softwareModuleTypeManagement;

    public JpaSoftwareModuleBuilder(final SoftwareModuleTypeManagement softwareModuleTypeManagement) {
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
    }

    @Override
    public SoftwareModuleUpdate update(final long id) {
        return new GenericSoftwareModuleUpdate(id);
    }

    @Override
    public SoftwareModuleCreate create() {
        return new JpaSoftwareModuleCreate(softwareModuleTypeManagement);
    }

}
