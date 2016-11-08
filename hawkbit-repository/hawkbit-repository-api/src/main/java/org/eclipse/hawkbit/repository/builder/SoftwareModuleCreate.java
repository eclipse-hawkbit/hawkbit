/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import java.util.Optional;

import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Builder to create a new {@link SoftwareModule} entry. Defines all fields that
 * can be set at creation time. Other fields are set by the repository
 * automatically, e.g. {@link BaseEntity#getCreatedAt()}.
 *
 */
public interface SoftwareModuleCreate {
    /**
     * @param name
     *            for {@link SoftwareModule#getName()}
     * @return updated builder instance
     */
    SoftwareModuleCreate name(@NotEmpty String name);

    /**
     * @param version
     *            for {@link SoftwareModule#getVersion()}
     * @return updated builder instance
     */
    SoftwareModuleCreate version(@NotEmpty String version);

    /**
     * @param description
     *            for {@link SoftwareModule#getDescription()}
     * @return updated builder instance
     */
    SoftwareModuleCreate description(String description);

    /**
     * @param vendor
     *            for {@link SoftwareModule#getVendor()}
     * @return updated builder instance
     */
    SoftwareModuleCreate vendor(String vendor);

    /**
     * @param typeKey
     *            for {@link SoftwareModule#getType()}
     * @return updated builder instance
     */
    SoftwareModuleCreate type(@NotEmpty String typeKey);

    /**
     * @param type
     *            for {@link SoftwareModule#getType()}
     * @return updated builder instance
     */
    default SoftwareModuleCreate type(final SoftwareModuleType type) {
        return type(Optional.ofNullable(type).map(SoftwareModuleType::getKey).orElse(null));
    }

    /**
     * @return peek on current state of {@link SoftwareModule} in the builder
     */
    SoftwareModule build();
}
