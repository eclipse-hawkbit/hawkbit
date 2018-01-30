/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;

/**
 * Builder for {@link SoftwareModuleMetadata}.
 *
 */
public interface SoftwareModuleMetadataBuilder {

    /**
     * @param softwareModuleId
     *            of the {@link SoftwareModule} the {@link MetaData} belongs to
     * @param key
     *            of {@link MetaData#getKey()}
     * @return builder instance
     */
    SoftwareModuleMetadataUpdate update(long softwareModuleId, String key);

    /**
     * @param softwareModuleId
     *            of the {@link SoftwareModule} the {@link MetaData} belongs to
     * @return builder instance
     */
    SoftwareModuleMetadataCreate create(long softwareModuleId);

}
