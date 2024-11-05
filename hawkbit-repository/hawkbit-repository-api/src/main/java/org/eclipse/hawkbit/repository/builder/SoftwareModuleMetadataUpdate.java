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

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;

/**
 * Builder to update an existing {@link SoftwareModuleMetadata} entry. Defines
 * all fields that can be updated.
 */
public interface SoftwareModuleMetadataUpdate {

    /**
     * @param value for {@link MetaData#getValue()}
     * @return updated builder instance
     */
    SoftwareModuleMetadataUpdate value(@Size(min = 1, max = MetaData.VALUE_MAX_SIZE) @NotNull String value);

    /**
     * @param visible for {@link SoftwareModuleMetadata#isTargetVisible()}
     * @return updated builder instance
     */
    SoftwareModuleMetadataUpdate targetVisible(Boolean visible);

}
