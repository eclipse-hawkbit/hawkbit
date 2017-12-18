/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

/**
 * Update implementation.
 */
public class GenericSoftwareModuleMetadataUpdate
        extends AbstractSoftwareModuleMetadataUpdateCreate<SoftwareModuleMetadataUpdate>
        implements SoftwareModuleMetadataUpdate {

    public GenericSoftwareModuleMetadataUpdate(final long softwareModuleId, final String key) {
        super.softwareModuleId = softwareModuleId;
        this.key = key;
    }

}
