/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
     * @param name
     *            the name of the enum
     * @return the corresponding enum
     * @throws SortParameterUnsupportedDirectionException
     *             if there is no matching enum for the specified name
     */
    public static SortDirection getByName(final String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (final IllegalArgumentException ex) {// NOSONAR
            throw new SortParameterUnsupportedDirectionException();
        }
    }
}
