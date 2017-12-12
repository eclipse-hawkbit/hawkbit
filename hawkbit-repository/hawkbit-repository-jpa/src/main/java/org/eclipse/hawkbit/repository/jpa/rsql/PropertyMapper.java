/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps property of entity to its allias .
 * 
 *
 *
 */
public final class PropertyMapper {

    private static Map<Class<?>, Map<String, String>> allowedColmns = new HashMap<>();

    private PropertyMapper() {

    }

    /**
     * Add new mapping - property name and alias.
     * 
     * @param type
     *            entity type
     * @param property
     *            alias of property
     * @param mapping
     *            property name
     */
    public static void addNewMapping(final Class<?> type, final String property, final String mapping) {
        allowedColmns.computeIfAbsent(type, k -> new HashMap<>());
        allowedColmns.get(type).put(property, mapping);
    }

    /**
     * @return the allowedcolmns
     */
    public static Map<Class<?>, Map<String, String>> getAllowedcolmns() {
        return allowedColmns;
    }

}
