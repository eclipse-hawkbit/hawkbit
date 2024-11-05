/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest.data;

import org.eclipse.hawkbit.rest.exception.SortParameterUnsupportedDirectionException;

/**
 * A definition of possible sorting direction.
 */
public enum SortDirection {
    /**
     * Ascending.
     */
    ASC,
    /**
     * Descending.
     */
    DESC;

    /**
     * Returns the sort direction for the given name.
     *
     * @param name the name of the enum
     * @return the corresponding enum
     * @throws SortParameterUnsupportedDirectionException if there is no matching enum for the specified name
     */
    public static SortDirection getByName(final String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (final IllegalArgumentException ex) {
            throw new SortParameterUnsupportedDirectionException();
        }
    }
}
