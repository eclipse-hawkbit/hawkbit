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

/**
 * An interface for declaring the name of the field described in the database
 * which is used as string representation of the field, e.g. for sorting the
 * fields over REST.
 *
 */
public interface FieldNameProvider {
    /**
     * Separator for the sub attributes
     */
    public static final String SUB_ATTRIBUTE_SEPERATOR = ".";

    /**
     * @return the string representation of the underlying persistence field
     *         name e.g. in case of sorting. Never {@code null}.
     */
    String getFieldName();

    default boolean containsSubEntityAttribute(final String propertyField) {
        return FieldNameProvider.containsSubEntityAttribute(propertyField, getSubEntityAttributes());
    };

    default List<String> getSubEntityAttributes() {
        return Collections.emptyList();
    };

    /**
     * the database column for the key
     * 
     * @return key fieldname
     */
    default String getKeyFieldName() {
        return null;
    }

    /**
     * the database column for the value
     * 
     * @return key fieldname
     */
    default String getValueFieldName() {
        return null;
    }

    /**
     * Is the entity field a {@link Map}.
     * 
     * @return
     */
    default boolean isMap() {
        return getKeyFieldName() != null;
    };

    /**
     * Check if a sub attribute exists.
     * 
     * @param propertyField
     *            the sub property field.
     * @param subEntityAttribues
     *            the list of available properties
     * @return <true> property exists <false> not exists
     */
    static boolean containsSubEntityAttribute(final String propertyField, final List<String> subEntityAttribues) {
        if (subEntityAttribues.contains(propertyField)) {
            return true;
        }
        for (final String attribute : subEntityAttribues) {
            final String[] graph = attribute.split("\\" + SUB_ATTRIBUTE_SEPERATOR);

            for (final String subAttribute : graph) {
                if (subAttribute.equalsIgnoreCase(propertyField)) {
                    return true;
                }
            }
        }

        return false;
    }

}
