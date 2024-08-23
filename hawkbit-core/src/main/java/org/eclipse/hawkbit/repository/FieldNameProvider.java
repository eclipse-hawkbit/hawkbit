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

import org.springframework.util.ObjectUtils;

/**
 * An interface for declaring the name of the field described in the database
 * which is used as string representation of the field, e.g. for sorting the
 * fields over REST.
 */
@FunctionalInterface
public interface FieldNameProvider {

    /**
     * Separator for the sub attributes
     */
    String SUB_ATTRIBUTE_SEPARATOR = ".";
    String SUB_ATTRIBUTE_SPLIT_REGEX = "\\" + SUB_ATTRIBUTE_SEPARATOR;

    /**
     * @return the string representation of the underlying persistence field
     *         name e.g. in case of sorting. Never {@code null}.
     */
    String getFieldName();

    /**
     * Returns the sub attributes
     *
     * @param propertyFieldName the given field
     * @return array consisting of sub attributes
     */
    default String[] getSubAttributes(final String propertyFieldName) {
        if (isMap()) {
            final String[] subAttributes = propertyFieldName.split(SUB_ATTRIBUTE_SPLIT_REGEX, 2);
            // [0] field name | [1] key name
            final String mapKeyName = subAttributes.length == 2 ? subAttributes[1] : null;
            if (ObjectUtils.isEmpty(mapKeyName)) {
                return new String[] { getFieldName() };
            }
            return new String[] { getFieldName(), mapKeyName };
        }
        return propertyFieldName.split(SUB_ATTRIBUTE_SPLIT_REGEX);
    }

    /**
     * Contains the sub entity the given field.
     *
     * @param propertyField the given field
     * @return <code>true</code> contains <code>false</code> contains not
     */
    default boolean containsSubEntityAttribute(final String propertyField) {
        final List<String> subEntityAttributes = getSubEntityAttributes();
        if (subEntityAttributes.contains(propertyField)) {
            return true;
        }

        for (final String attribute : subEntityAttributes) {
            final String[] graph = attribute.split(SUB_ATTRIBUTE_SPLIT_REGEX);
            for (final String subAttribute : graph) {
                if (subAttribute.equalsIgnoreCase(propertyField)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @return all sub entities attributes.
     */
    default List<String> getSubEntityAttributes() {
        return Collections.emptyList();
    }

    /**
     * @return a key/value tuple of a sub entity.
     */
    default Optional<Entry<String, String>> getSubEntityMapTuple() {
        return Optional.empty();
    }

    /**
     * Is the entity field a {@link Map} consisting of key-value pairs.
     *
     * @return <code>true</code> is a map <code>false</code> is not a map
     */
    default boolean isMap() {
        return getSubEntityMapTuple().isPresent();
    }

    /**
     * Returns the name of the field, that identifies the entity.
     *
     * @return the name of the identifier, by default 'id'
     */
    default String identifierFieldName() {
        return "id";
    }
}