/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import jakarta.validation.constraints.NotNull;

/**
 * An RSQL query field interface extended by all the fields that could be used in RSQL queries.
 */
public interface RsqlQueryField {

    /**
     * Separator for the sub attributes
     */
    String SUB_ATTRIBUTE_SEPARATOR = ".";
    String SUB_ATTRIBUTE_SPLIT_REGEX = "\\" + SUB_ATTRIBUTE_SEPARATOR;

    /**
     * @return the string representation of the underlying persistence field name e.g. in case of sorting.
     */
    @NotNull
    String getJpaEntityFieldName();

    /**
     * @return all sub entities attributes.
     */
    default List<String> getSubEntityAttributes() {
        return Collections.emptyList();
    }

    /**
     * Returns the name of the field, that identifies the entity.
     *
     * @return the name of the identifier, by default 'id'
     */
    default String identifierFieldName() {
        return "id";
    }

    /**
     * Is the entity field a {@link Map} consisting of key-value pairs.
     *
     * @return <code>true</code> is a map <code>false</code> is not a map
     */
    default boolean isMap() {
        return false;
    }
}