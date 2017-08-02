/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
