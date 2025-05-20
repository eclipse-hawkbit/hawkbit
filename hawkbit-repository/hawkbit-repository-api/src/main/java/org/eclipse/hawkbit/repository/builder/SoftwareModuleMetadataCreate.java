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

import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;

/**
 * Builder to create a new {@link SoftwareModuleMetadata} entry. Defines all
 * fields that can be set at creation time. Other fields are set by the
 * repository automatically, e.g. {@link BaseEntity#getCreatedAt()}.
 */
public interface SoftwareModuleMetadataCreate {

    /**
     * @param key for {@link SoftwareModuleMetadata#getKey()}
     * @return updated builder instance
     */
    SoftwareModuleMetadataCreate key(@Size(min = 1, max =SoftwareModule.METADATA_KEY_MAX_SIZE) @NotNull String key);

    /**
     * @param value for {@link SoftwareModuleMetadata#getValue()}
     * @return updated builder instance
     */
    SoftwareModuleMetadataCreate value(@Size(max = SoftwareModule.METADATA_VALUE_MAX_SIZE) String value);

    /**
     * @param visible for {@link SoftwareModuleMetadata#isTargetVisible()}
     * @return updated builder instance
     */
    SoftwareModuleMetadataCreate targetVisible(Boolean visible);

    /**
     * @return peek on current state of {@link SoftwareModuleMetadata} in the
     *         builder
     */
    SoftwareModuleMetadata build();
}