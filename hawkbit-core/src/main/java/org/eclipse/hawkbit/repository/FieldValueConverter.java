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
 * 'finished' values in rest queries to Action#isActive boolean value.
 *
 * @param <T> the enum parameter
 */
public interface FieldValueConverter<T extends Enum<T>> {

    /**
     * Converts the given {@code value} into the representation to build ageneric query.
     *
     * @param enumValue the enum value to build the value for
     * @param value the value in string representation
     * @return the converted object if conversion is applicable, or if the given enum value does not need to be converted the
     *         unmodified {@code value} is returned.
     * @throws IllegalArgumentException if the value is not supported
     */
    Object convertValue(final T enumValue, final String value);
}