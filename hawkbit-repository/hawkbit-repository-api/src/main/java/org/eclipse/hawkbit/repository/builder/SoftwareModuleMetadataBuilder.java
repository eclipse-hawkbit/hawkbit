/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.builder;

import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;

/**
 * Builder for {@link SoftwareModuleMetadata}.
 */
public interface SoftwareModuleMetadataBuilder {

    /**
     * @param softwareModuleId of the {@link SoftwareModule} the {@link MetaData} belongs to
     * @param key of {@link MetaData#getKey()}
     * @return builder instance
     */
    SoftwareModuleMetadataUpdate update(long softwareModuleId, String key);

    /**
     * @param softwareModuleId of the {@link SoftwareModule} the {@link MetaData} belongs to
     * @return builder instance
     */
    SoftwareModuleMetadataCreate create(long softwareModuleId);
}