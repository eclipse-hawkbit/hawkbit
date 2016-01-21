/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

/**
 * A value convert which converts given string based values into an object which
 * can be used for building generic queries. Mapping external API values e.g.
 * REST API to inside representation on database. E.g. mapping 'pending' or
 * 'finished' values in rest queries to {@link TargetAction#isActive()} boolean
 * value.
 * 
 *
 *
 * @param <T>
 *            the enum parameter
 *
 */
public interface FieldValueConverter<T extends Enum<T>> {

    /**
     * converts the given {@code value} into the representation to build a
     * generic query.
     * 
     * @param e
     *            the enum to build the value for
     * @param value
     *            the value in string representation
     * @return the converted object or {@code null} if conversation fails, if
     *         given enum does not need to be converted the the unmodified
     *         {@code value} is returned.
     */
    Object convertValue(final T e, final String value);

    /**
     * returns the possible values associated with the given enum type.
     * 
     * @param e
     *            the enum type to retrieve the possible values
     * @return the possible values for a specific enum or {@code null}
     */
    String[] possibleValues(final T e);

}
