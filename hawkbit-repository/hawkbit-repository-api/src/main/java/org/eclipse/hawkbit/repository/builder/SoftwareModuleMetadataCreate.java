/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;

/**
 * Builder to create a new {@link SoftwareModuleMetadata} entry. Defines all
 * fields that can be set at creation time. Other fields are set by the
 * repository automatically, e.g. {@link BaseEntity#getCreatedAt()}.
 *
 */
public interface SoftwareModuleMetadataCreate {

    /**
     * @param key
     *            for {@link MetaData#getKey()}
     * @return updated builder instance
     */
    SoftwareModuleMetadataCreate key(@Size(min = 1, max = MetaData.KEY_MAX_SIZE) @NotNull String key);

    /**
     * @param value
     *            for {@link MetaData#getValue()}
     * @return updated builder instance
     */
    SoftwareModuleMetadataCreate value(@Size(min = 1, max = MetaData.VALUE_MAX_SIZE) @NotNull String value);

    /**
     * @param visible
     *            for {@link SoftwareModuleMetadata#isTargetVisible()}
     * @return updated builder instance
     */
    SoftwareModuleMetadataCreate targetVisible(Boolean visible);

    /**
     * @return peek on current state of {@link SoftwareModuleMetadata} in the
     *         builder
     */
    SoftwareModuleMetadata build();

}
