/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ql;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.NonNull;

/**
 * A query field interface extended by all the fields that could be used in queries.
 */
public interface QueryField {

    /**
     * Separator for the sub attributes
     */
    String SUB_ATTRIBUTE_SEPARATOR = ".";
    String SUB_ATTRIBUTE_SPLIT_REGEX = "\\" + SUB_ATTRIBUTE_SEPARATOR;

    /**
     * Returns the entity field name, e.g. the JPA entity field name.
     *
     * @return the string representation of the underlying persistence field name.
     */
    @NonNull
    String getName();

    /**
     * @return all sub entities attributes.
     */
    default List<String> getSubEntityAttributes() {
        return Collections.emptyList();
    }

    /**
     * Return the default sub entity attribute if available. This allows to skip that sub-attribute and call with "shortcut" - the enum name
     *
     * @return the default sub-attribute or <code>null</code>> if no default is available
     */
    default String getDefaultSubEntityAttribute() {
        final List<String> subAttributes = getSubEntityAttributes();
        return subAttributes.size() == 1 ? subAttributes.get(0) : null;
    }

    /**
     * Return the default attribute of a sub entity attribute which is an entity itself, if available. This allows to skip the nested
     * attribute and call with "shortcut" - e.g. <code>distributionSet.type==os</code> instead of <code>distributionSet.type.key==os</code>
     *
     * @param subEntityAttribute the sub entity attribute (as returned by {@link #getSubEntityAttributes()})
     * @return the default attribute of the sub entity attribute or <code>null</code> if no default is available
     */
    default String getDefaultSubEntityAttribute(final String subEntityAttribute) {
        return null;
    }

    /**
     * Returns the default sub entity attribute of the field of the given query field type with the given name. Helps query fields
     * to resolve their {@link #getDefaultSubEntityAttribute(String)} using the query field type of the sub entity.
     *
     * @param queryFieldType the query field type of the sub entity
     * @param name the (entity field) name of the field
     * @return the default sub entity attribute or <code>null</code> if there is no such field or it has no default sub attribute
     */
    static <T extends Enum<T> & QueryField> String defaultSubEntityAttributeOf(final Class<T> queryFieldType, final String name) {
        for (final T field : queryFieldType.getEnumConstants()) {
            if (field.getName().equals(name) && !field.isMap() && !field.getSubEntityAttributes().isEmpty()) {
                return field.getDefaultSubEntityAttribute();
            }
        }
        return null;
    }

    /**
     * Returns the name of the field, that identifies the entity.
     *
     * @return the name of the identifier, by default 'id'
     */
    default String getIdentifierFieldName() {
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
