/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

/**
 * Entities that have a name and description.
 */
public interface NamedEntity extends TenantAwareBaseEntity {

    /**
     * Maximum length of name.
     */
    int NAME_MAX_SIZE = 128;

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
