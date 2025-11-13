/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.mgmt.rest.resource.exception.SortParameterSyntaxErrorException;
import org.eclipse.hawkbit.mgmt.rest.resource.exception.SortParameterUnsupportedDirectionException;
import org.eclipse.hawkbit.mgmt.rest.resource.exception.SortParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.qfields.QueryField;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;

/**
 * A utility class for parsing query parameters which define the sorting of elements.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SortUtility {

    /**
     * the delimiter between the different field directions in the sort request.
     */
    public static final String DELIMITER_SORT_TUPLE = ",";
    /**
     * the delimiter between the field and direction in the sort request.
     */
    public static final String DELIMITER_FIELD_DIRECTION = ":";

    /**
     * Parses the sort string e.g. given in a REST call based on the definition
     * of sorting: <code>http://localhost/entity?s=field1:ASC,field2:DESC</code> The fields
     * will be split into the keys of the returned map. The direction of the
     * sorting will be mapped into the {@link Direction} enum.
     *
     * @param enumType the class of the enum which the fields in the sort string should be related to.
     * @param <T> the type of the enumeration which must be derived from {@link QueryField}
     * @param sortString the string representation of the query parameters. Might be {@code null} or an empty string.
     * @return a list which holds the {@link QueryField} and the specific {@link Direction} for them as a tuple. Never {@code null}.
     *         In case of no sorting parameters an empty map will be returned.
     * @throws SortParameterSyntaxErrorException if the sorting query parameter is not well-formed
     * @throws SortParameterUnsupportedFieldException if a field name cannot be mapped to the enum type
     * @throws SortParameterUnsupportedDirectionException if the given direction is not "ASC" or "DESC"
     */
    public static <T extends Enum<T> & QueryField> List<Order> parse(final Class<T> enumType, final String sortString)
            throws SortParameterSyntaxErrorException {
        final List<Order> orders = new ArrayList<>();
        // scan the sort tuples e.g. field:direction
        if (sortString != null) {
            final StringTokenizer tupleTokenizer = new StringTokenizer(sortString, DELIMITER_SORT_TUPLE);
            while (tupleTokenizer.hasMoreTokens()) {
                final String sortTuple = tupleTokenizer.nextToken().trim();
                final StringTokenizer fieldDirectionTokenizer = new StringTokenizer(sortTuple, DELIMITER_FIELD_DIRECTION);
                if (fieldDirectionTokenizer.countTokens() == 2) {
                    final String fieldName = fieldDirectionTokenizer.nextToken().trim().toUpperCase();
                    final String sortDirectionStr = fieldDirectionTokenizer.nextToken().trim();

                    final T identifier;
                    try {
                        identifier = Enum.valueOf(enumType, fieldName.toUpperCase());
                    } catch (final IllegalArgumentException e) {
                        throw new SortParameterUnsupportedFieldException(e);
                    }

                    final Direction sortDirection;
                    try {
                        sortDirection = Direction.fromString(sortDirectionStr);
                    } catch (final IllegalArgumentException e) {
                        throw new SortParameterUnsupportedDirectionException(e);
                    }

                    orders.add(new Order(sortDirection, identifier.getJpaEntityFieldName()));
                } else {
                    throw new SortParameterSyntaxErrorException();
                }
            }
        }
        return orders;
    }
}