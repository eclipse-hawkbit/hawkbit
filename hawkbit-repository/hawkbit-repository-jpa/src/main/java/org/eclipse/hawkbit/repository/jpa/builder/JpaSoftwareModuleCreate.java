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

import jakarta.validation.ValidationException;

import org.eclipse.hawkbit.repository.RepositoryManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.builder.AbstractSoftwareModuleUpdateCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.management.JpaSoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * Create/build implementation.
 */
public class JpaSoftwareModuleCreate
        extends AbstractSoftwareModuleUpdateCreate<SoftwareModuleCreate<JpaSoftwareModule>>
        implements SoftwareModuleCreate<JpaSoftwareModule> {

    private final JpaSoftwareModuleTypeManagement softwareModuleTypeManagement;
    private boolean encrypted;

    JpaSoftwareModuleCreate(final JpaSoftwareModuleTypeManagement softwareModuleTypeManagement) {
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
    }

    @Override
    public SoftwareModuleCreate encrypted(final boolean encrypted) {
        this.encrypted = encrypted;
        return this;
    }

    @Override
    public JpaSoftwareModule build() {
        return new JpaSoftwareModule(getSoftwareModuleTypeFromKeyString(type), name, version, description, vendor,
                encrypted);
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    private SoftwareModuleType getSoftwareModuleTypeFromKeyString(final String type) {
        if (type == null) {
            throw new ValidationException("type cannot be null");
        }

        return softwareModuleTypeManagement.findByKey(type.trim())
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModuleType.class, type.trim()));
    }
}