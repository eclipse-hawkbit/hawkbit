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

import jakarta.annotation.Nullable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Update implementation.
 */
@Data
@Accessors(fluent = true) // override locked()
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class GenericSoftwareModuleUpdate extends AbstractSoftwareModuleUpdateCreate<SoftwareModuleUpdate>
        implements SoftwareModuleUpdate {

    @Nullable
    protected Boolean locked;

    public GenericSoftwareModuleUpdate(final Long id) {
        super.id = id;
    }
}