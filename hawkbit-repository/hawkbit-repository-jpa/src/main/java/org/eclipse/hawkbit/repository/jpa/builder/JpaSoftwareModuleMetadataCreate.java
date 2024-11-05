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

import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.builder.AbstractSoftwareModuleMetadataUpdateCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModule;

/**
 * Create/build implementation.
 */
public class JpaSoftwareModuleMetadataCreate
        extends AbstractSoftwareModuleMetadataUpdateCreate<SoftwareModuleMetadataCreate>
        implements SoftwareModuleMetadataCreate {

    private final SoftwareModuleManagement softwareModuleManagement;

    JpaSoftwareModuleMetadataCreate(final long softwareModuleId,
            final SoftwareModuleManagement softwareModuleManagement) {
        this.softwareModuleManagement = softwareModuleManagement;
        this.softwareModuleId = softwareModuleId;
    }

    @Override
    public JpaSoftwareModuleMetadata build() {
        final SoftwareModule module = softwareModuleManagement.get(softwareModuleId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, softwareModuleId));

        if (key == null) {
            new JpaSoftwareModuleMetadata(key, module, value, isTargetVisible().orElse(false));
        }

        return new JpaSoftwareModuleMetadata(key, module, value, isTargetVisible().orElse(false));
    }

}
