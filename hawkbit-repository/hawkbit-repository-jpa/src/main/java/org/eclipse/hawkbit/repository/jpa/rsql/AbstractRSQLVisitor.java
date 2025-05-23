/**
 * Copyright (c) 2020 devolo AG and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import jakarta.validation.constraints.NotNull;

import cz.jirutka.rsql.parser.ast.ComparisonNode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.RsqlQueryField;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.springframework.util.ObjectUtils;

@Slf4j
public abstract class AbstractRSQLVisitor<A extends Enum<A> & RsqlQueryField> {

    private final Class<A> rsqlQueryFieldType;

    protected AbstractRSQLVisitor(final Class<A> rsqlQueryFieldType) {
        this.rsqlQueryFieldType = rsqlQueryFieldType;
    }

    @SuppressWarnings("java:S1066") // java:S1066 - more readable with separate "if" statements
    protected QueryPath getQueryPath(final ComparisonNode node) {
        final int firstSeparatorIndex = node.getSelector().indexOf(RsqlQueryField.SUB_ATTRIBUTE_SEPARATOR);
        final String enumName = (firstSeparatorIndex == -1
                ? node.getSelector()
                : node.getSelector().substring(0, firstSeparatorIndex)).toUpperCase();
        log.debug("Get field identifier by name {} of enum type {}", enumName, rsqlQueryFieldType);

        try {
            final A enumValue = Enum.valueOf(rsqlQueryFieldType, enumName);
            String[] split = getSplit(enumValue, node.getSelector());

            // validate
            if (!enumValue.isMap()) {
                // sub entity need minimum 1 dot
                if (!enumValue.getSubEntityAttributes().isEmpty() && split.length < 2) {
                    if (enumValue.getSubEntityAttributes().size() == 1) { // single sub attribute - so add is as a default
                        split = new String[] { split[0], enumValue.getSubEntityAttributes().get(0) };
                    } else {
                        throw createRSQLParameterUnsupportedException(node, null);
                    }
                }
            }

            // validate and normalize (replace enum name with JPA entity field name and format the rest) prepare jpa path
            split[0] = enumValue.getJpaEntityFieldName();
            for (int i = 1; i < split.length; i++) {
                split[i] = getFormattedSubEntityAttribute(enumValue, split[i]);

                if (!containsSubEntityAttribute(enumValue, split[i])) {
                    if (i != split.length - 1 || !enumValue.isMap()) {
                        throw createRSQLParameterUnsupportedException(node, null);
                    } // otherwise - the key of map is not in the sub entity attributes
                }
            }

            return new QueryPath(enumValue, split);
        } catch (final IllegalArgumentException e) {
            throw createRSQLParameterUnsupportedException(node, e);
        }
    }

    private String[] getSplit(final A enumValue, final String rsqlFieldName) {
        if (enumValue.isMap()) {
            final String[] split = rsqlFieldName.split(RsqlQueryField.SUB_ATTRIBUTE_SPLIT_REGEX, 2);
            if (split.length != 2 || ObjectUtils.isEmpty(split[1])) {
                throw new RSQLParameterUnsupportedFieldException(
                        "The syntax of the given map search parameter field {" + rsqlFieldName + "} is wrong. Syntax is: <enum name>.<key name>");
            }
            return split;
        } else {
            return rsqlFieldName.split(RsqlQueryField.SUB_ATTRIBUTE_SPLIT_REGEX);
        }
    }



    /**
     * Contains the sub entity the given field.
     *
     * @param propertyField the given field
     * @return <code>true</code> contains <code>false</code> contains not
     */
    private boolean containsSubEntityAttribute(final A enumField, final String propertyField) {
        final List<String> subEntityAttributes = enumField.getSubEntityAttributes();
        if (subEntityAttributes.contains(propertyField)) {
            return true;
        }

        for (final String attribute : subEntityAttributes) {
            final String[] graph = attribute.split(RsqlQueryField.SUB_ATTRIBUTE_SPLIT_REGEX);
            for (final String subAttribute : graph) {
                if (subAttribute.equalsIgnoreCase(propertyField)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @param node current processing node
     * @param rootException in case there is a cause otherwise {@code null}
     * @return Exception with prepared message extracted from the comparison node.
     */
    private RSQLParameterUnsupportedFieldException createRSQLParameterUnsupportedException(
            @NotNull final ComparisonNode node, final Exception rootException) {
        return new RSQLParameterUnsupportedFieldException(String.format(
                "The given search parameter field {%s} does not exist, must be one of the following fields %s",
                node.getSelector(), getExpectedFieldList()), rootException);
    }

    private String getFormattedSubEntityAttribute(final A propertyEnum, final String propertyField) {
        return propertyEnum.getSubEntityAttributes().stream()
                .filter(attr -> attr.equalsIgnoreCase(propertyField))
                .findFirst().orElse(propertyField);
    }

    private List<String> getExpectedFieldList() {
        return Stream.concat(
                Arrays.stream(rsqlQueryFieldType.getEnumConstants())
                .filter(enumField -> enumField.getSubEntityAttributes().isEmpty()).map(enumField -> {
                    final String enumFieldName = enumField.name().toLowerCase();
                    if (enumField.isMap()) {
                        return enumFieldName + RsqlQueryField.SUB_ATTRIBUTE_SEPARATOR + "keyName";
                    } else {
                        return enumFieldName;
                    }
                }),
                Arrays.stream(rsqlQueryFieldType.getEnumConstants())
                .filter(enumField -> !enumField.getSubEntityAttributes().isEmpty()).flatMap(enumField -> {
                    final List<String> subEntity = enumField
                            .getSubEntityAttributes().stream().map(fieldName -> enumField.name().toLowerCase()
                                    + RsqlQueryField.SUB_ATTRIBUTE_SEPARATOR + fieldName)
                            .toList();

                    return subEntity.stream();
                }))
                .toList();
    }

    @Value
    protected class QueryPath {

        A enumValue;
        String[] jpaPath;

        private QueryPath(final A enumValue, final String[] jpaPath) {
            this.enumValue = enumValue;
            this.jpaPath = jpaPath;
        }
    }
}