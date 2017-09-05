/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import javax.validation.ValidationException;

import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.builder.AbstractSoftwareModuleUpdateCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * Create/build implementation.
 *
 */
public class JpaSoftwareModuleCreate extends AbstractSoftwareModuleUpdateCreate<SoftwareModuleCreate>
        implements SoftwareModuleCreate {

    private final SoftwareModuleTypeManagement softwareModuleTypeManagement;

    JpaSoftwareModuleCreate(final SoftwareModuleTypeManagement softwareModuleTypeManagement) {
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
    }

    @Override
    public JpaSoftwareModule build() {
        return new JpaSoftwareModule(getSoftwareModuleTypeFromKeyString(type), name, version, description, vendor);
    }

    private SoftwareModuleType getSoftwareModuleTypeFromKeyString(final String type) {
        if (type == null) {
            throw new ValidationException("type cannot be null");
        }

        return softwareModuleTypeManagement.getByKey(type.trim())
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModuleType.class, type.trim()));
    }
}
