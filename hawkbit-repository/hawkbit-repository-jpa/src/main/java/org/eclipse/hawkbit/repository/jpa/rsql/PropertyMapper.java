/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps property of entity to its alias .
 */
public final class PropertyMapper {

    private static Map<Class<?>, Map<String, String>> allowedColumns = new HashMap<>();

    private PropertyMapper() {

    }

    /**
     * Add new mapping - property name and alias.
     *
     * @param type entity type
     * @param property alias of property
     * @param mapping property name
     */
    public static void addNewMapping(final Class<?> type, final String property, final String mapping) {
        allowedColumns.computeIfAbsent(type, k -> new HashMap<>());
        allowedColumns.get(type).put(property, mapping);
    }

    /**
     * @return the allowedcolmns
     */
    public static Map<Class<?>, Map<String, String>> getAllowedcolmns() {
        return allowedColumns;
    }

}
