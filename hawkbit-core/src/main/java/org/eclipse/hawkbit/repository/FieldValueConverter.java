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

/**
 * A value convert which converts given string based values into an object which
 * can be used for building generic queries. Mapping external API values e.g.
 * REST API to inside representation on database. E.g. mapping 'pending' or
 * 'finished' values in rest queries to Action#isActive boolean
 * value.
 *
 * @param <T> the enum parameter
 */
public interface FieldValueConverter<T extends Enum<T>> {

    /**
     * Converts the given {@code value} into the representation to build a
     * generic query.
     *
     * @param e the enum to build the value for
     * @param value the value in string representation
     * @return the converted object or {@code null} if conversation fails, if
     *         given enum does not need to be converted the the unmodified
     *         {@code value} is returned.
     */
    Object convertValue(final T e, final String value);

    /**
     * returns the possible values associated with the given enum type.
     *
     * @param e the enum type to retrieve the possible values
     * @return the possible values for a specific enum or {@code null}
     */
    String[] possibleValues(final T e);
}