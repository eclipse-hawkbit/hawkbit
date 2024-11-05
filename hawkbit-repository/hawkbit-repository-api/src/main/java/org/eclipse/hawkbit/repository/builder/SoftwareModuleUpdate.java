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

import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;

import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModule;

/**
 * Builder to update an existing {@link SoftwareModule} entry. Defines all
 * fields that can be updated.
 */
public interface SoftwareModuleUpdate {

    /**
     * @param description for {@link SoftwareModule#getDescription()}
     * @return updated builder instance
     */
    SoftwareModuleUpdate description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

    /**
     * @param vendor for {@link SoftwareModule#getVendor()}
     * @return updated builder instance
     */
    SoftwareModuleUpdate vendor(@Size(max = SoftwareModule.VENDOR_MAX_SIZE) String vendor);

    /**
     * @param locked update request if any. If not empty shall be <code>true</code>
     * @return updated builder instance
     */
    SoftwareModuleUpdate locked(@Null Boolean locked);
}