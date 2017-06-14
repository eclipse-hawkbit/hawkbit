/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Entities that have a name and description.
 *
 */
public interface NamedEntity extends TenantAwareBaseEntity {
    /**
     * Maximum length of name.
     */
    public static final int NAME_MAX_SIZE = 64;

    /**
     * Maximum length of description.
     */
    public static final int DESCRIPTION_MAX_SIZE = 512;

    /**
     * @return the description of the entity.
     */
    @Size(max = DESCRIPTION_MAX_SIZE)
    String getDescription();

    /**
     * @return the name of the entity.
     */
    @Size(min = 1, max = NAME_MAX_SIZE)
    @NotNull
    String getName();
}
