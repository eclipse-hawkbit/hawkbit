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

import java.util.Optional;

/**
 * Update implementation.
 */
public class GenericSoftwareModuleUpdate extends AbstractSoftwareModuleUpdateCreate<SoftwareModuleUpdate>
        implements SoftwareModuleUpdate {

    @Nullable
    protected Boolean locked;

    public GenericSoftwareModuleUpdate(final Long id) {
        super.id = id;
    }

    public SoftwareModuleUpdate locked(@Nullable final Boolean locked) {
        if (Boolean.FALSE.equals(locked)) {
            this.locked = null;
        } else {
            this.locked = locked;
        }
        return this;
    }

    public Boolean getLocked() {
        return locked;
    }
}