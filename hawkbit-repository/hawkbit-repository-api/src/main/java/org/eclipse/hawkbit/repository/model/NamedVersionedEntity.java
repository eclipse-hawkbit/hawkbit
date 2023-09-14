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
 * Entities that have a name and a description.
 *
 */
public interface NamedVersionedEntity extends NamedEntity {
    /**
     * Maximum length of version.
     */
    int VERSION_MAX_SIZE = 64;

    /**
     * @return the version of entity.
     */
    String getVersion();
}
