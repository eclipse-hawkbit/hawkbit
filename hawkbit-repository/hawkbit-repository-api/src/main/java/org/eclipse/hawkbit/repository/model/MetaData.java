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

import java.io.Serializable;

/**
 * Meta data for entities, a (key/value) store.
 *
 */
public interface MetaData extends Serializable {
    /**
     * Maximum length of metadata key.
     */
    int KEY_MAX_SIZE = 128;

    /**
     * Maximum length of metadata value.
     */
    int VALUE_MAX_SIZE = 4000;

    /**
     * @return {@link BaseEntity#getId()} the metadata is related to
     */
    Long getEntityId();

    /**
     * @return the key
     */
    String getKey();

    /**
     * @return the value
     */
    String getValue();

}
