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
import org.eclipse.hawkbit.repository.builder.GenericSoftwareModuleMetadataUpdate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataBuilder;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataUpdate;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;

/**
 * Builder implementation for {@link SoftwareModuleMetadata}.
 */
public class JpaSoftwareModuleMetadataBuilder implements SoftwareModuleMetadataBuilder {

    private final SoftwareModuleManagement softwareModuleManagement;

    public JpaSoftwareModuleMetadataBuilder(final SoftwareModuleManagement softwareModuleManagement) {
        this.softwareModuleManagement = softwareModuleManagement;
    }

    @Override
    public SoftwareModuleMetadataUpdate update(final long softwareModuleId, final String key) {
        return new GenericSoftwareModuleMetadataUpdate(softwareModuleId, key);
    }

    @Override
    public SoftwareModuleMetadataCreate create(final long softwareModuleId) {
        return new JpaSoftwareModuleMetadataCreate(softwareModuleId, softwareModuleManagement);
    }

}
