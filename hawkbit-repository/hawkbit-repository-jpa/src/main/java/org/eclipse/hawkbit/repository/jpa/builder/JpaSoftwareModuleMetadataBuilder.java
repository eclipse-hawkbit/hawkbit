/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 *
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
