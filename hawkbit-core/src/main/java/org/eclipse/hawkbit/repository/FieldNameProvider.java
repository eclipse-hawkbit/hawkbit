/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.springframework.util.StringUtils;

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

    /**
     * @return the string representation of the underlying persistence field
     *         name e.g. in case of sorting. Never {@code null}.
     */
    String getFieldName();

    /**
     * Returns the sub attributes
     *
     * @param propertyFieldName
     *            the given field
     * @return array consisting of sub attributes
     */
    default String[] getSubAttributes(final String propertyFieldName) {
        if (isMap()) {
            final String[] subAttributes = propertyFieldName.split("\\" + SUB_ATTRIBUTE_SEPARATOR, 2);
            // [0] fieldname |[1] keyname
            final String mapKeyName = subAttributes.length == 2 ? subAttributes[1] : null;
            if (StringUtils.isEmpty(mapKeyName)) {
                return new String[] { getFieldName() };
            }
            return new String[] { getFieldName(), mapKeyName };
        }
        return propertyFieldName.split("\\" + SUB_ATTRIBUTE_SEPARATOR);
    }

    /**
     * Contains the sub entity the given field.
     *
     * @param propertyField
     *            the given field
     * @return <code>true</code> contains <code>false</code> contains not
     */
    default boolean containsSubEntityAttribute(final String propertyField) {

        final List<String> subEntityAttributes = getSubEntityAttributes();
        if (subEntityAttributes.contains(propertyField)) {
            return true;
        }
        for (final String attribute : subEntityAttributes) {
            final String[] graph = attribute.split("\\" + SUB_ATTRIBUTE_SEPARATOR);

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
        return false;
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
