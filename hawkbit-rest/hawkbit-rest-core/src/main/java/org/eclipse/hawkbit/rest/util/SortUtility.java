/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.hawkbit.repository.FieldNameProvider;
import org.eclipse.hawkbit.rest.exception.SortParameterSyntaxErrorException;
import org.eclipse.hawkbit.rest.exception.SortParameterUnsupportedDirectionException;
import org.eclipse.hawkbit.rest.exception.SortParameterUnsupportedFieldException;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;

/**
 * A utility class for parsing query parameters which define the sorting of
 * elements.
 * 
 *
 *
 */
public final class SortUtility {

    /**
     * the delimiter between the field and direction in the sort request.
     */
    public static final String DELIMITER_FIELD_DIRECTION = ":";

    private static final String DELIMITER_SORT_TUPLE = ",";

    /*
     * utility constructor private.
     */
    private SortUtility() {
    }

    /**
     * Parses the sort string e.g. given in a REST call based on the definition
     * of sorting: http://localhost/entity?s=field1:ASC, field2:DESC The fields
     * will be split into the keys of the returned map. The direction of the
     * sorting will be mapped into the {@link Direction} enum.
     * 
     * @param enumType
     *            the class of the enum which the fields in the sort string
     *            should be related to.
     * @param <T>
     *            the type of the enumeration which must be derived from
     *            {@link FieldNameProvider}
     * @param sortString
     *            the string representation of the query parameters. Might be
     *            {@code null} or an empty string.
     * @return a list which holds the {@link FieldNameProvider} and the specific
     *         {@link Direction} for them as a tuple. Never {@code null}. In
     *         case of no sorting parameters an empty map will be returned.
     * @throws SortParameterSyntaxErrorException
     *             if the sorting query parameter is not well-formed
     * @throws SortParameterUnsupportedFieldException
     *             if a field name cannot be mapped to the enum type
     * @throws SortParameterUnsupportedDirectionException
     *             if the given direction is not "ASC" or "DESC"
     */
    public static <T extends Enum<T> & FieldNameProvider> List<Order> parse(final Class<T> enumType,
            final String sortString) throws SortParameterSyntaxErrorException {
        final List<Order> parsedSortings = new ArrayList<>();
        // scan the sort tuples e.g. field:direction
        if (sortString != null) {
            final StringTokenizer tupleTokenizer = new StringTokenizer(sortString, DELIMITER_SORT_TUPLE);
            while (tupleTokenizer.hasMoreTokens()) {
                final String sortTuple = tupleTokenizer.nextToken().trim();
                final StringTokenizer fieldDirectionTokenizer = new StringTokenizer(sortTuple,
                        DELIMITER_FIELD_DIRECTION);
                if (fieldDirectionTokenizer.countTokens() == 2) {
                    final String fieldName = fieldDirectionTokenizer.nextToken().trim().toUpperCase();
                    final String sortDirectionStr = fieldDirectionTokenizer.nextToken().trim();

                    final T identifier = getAttributeIdentifierByName(enumType, fieldName);

                    final Direction sortDirection = getDirection(sortDirectionStr);
                    parsedSortings.add(new Order(sortDirection, identifier.getFieldName()));
                } else {
                    throw new SortParameterSyntaxErrorException();
                }
            }
        }
        return parsedSortings;
    }

    /**
     * Returns the attribute identifier for the given name.
     * 
     * @param enumType
     *            the class of the enum which the fields in the sort string
     *            should be related to.
     * @param name
     *            the name of the enum
     * @param <T>
     *            the type of the enumeration which must be derived from
     *            {@link FieldNameProvider}
     * @return the corresponding enum
     * @throws SortParameterUnsupportedFieldException
     *             if there is no matching enum for the specified name
     */
    private static <T extends Enum<T> & FieldNameProvider> T getAttributeIdentifierByName(final Class<T> enumType,
            final String name) {
        try {
            return Enum.valueOf(enumType, name.toUpperCase());
        } catch (final IllegalArgumentException e) {
            throw new SortParameterUnsupportedFieldException(e);
        }
    }

    private static Direction getDirection(final String sortDirectionStr) {
        try {
            return Direction.fromString(sortDirectionStr);
        } catch (final IllegalArgumentException e) {
            throw new SortParameterUnsupportedDirectionException(e);
        }
    }
}
