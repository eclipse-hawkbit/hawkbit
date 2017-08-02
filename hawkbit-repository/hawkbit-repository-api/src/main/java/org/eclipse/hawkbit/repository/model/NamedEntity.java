/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

/**
 * Entities that have a name and description.
 *
 */
public interface NamedEntity extends TenantAwareBaseEntity {
    /**
     * Maximum length of name.
     */
    int NAME_MAX_SIZE = 64;

    /**
     * Maximum length of description.
     */
    int DESCRIPTION_MAX_SIZE = 512;

    /**
     * @return the description of the entity.
     */
    String getDescription();

    /**
     * @return the name of the entity.
     */
    String getName();
}
