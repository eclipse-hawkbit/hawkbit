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

import javax.validation.constraints.Size;

import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * Builder to update an existing {@link SoftwareModuleType} entry. Defines all
 * fields that can be updated.
 *
 */
public interface SoftwareModuleTypeUpdate {
    /**
     * @param description
     *            for {@link SoftwareModuleType#getDescription()}
     * @return updated builder instance
     */
    SoftwareModuleTypeUpdate description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

    /**
     * @param colour
     *            for {@link SoftwareModuleType#getColour()}
     * @return updated builder instance
     */
    SoftwareModuleTypeUpdate colour(@Size(max = SoftwareModuleType.COLOUR_MAX_SIZE) String colour);
}
