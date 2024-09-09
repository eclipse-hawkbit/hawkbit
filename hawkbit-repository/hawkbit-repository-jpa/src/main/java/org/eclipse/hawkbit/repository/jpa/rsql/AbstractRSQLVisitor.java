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
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.RsqlQueryField;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;

import cz.jirutka.rsql.parser.ast.ComparisonNode;

@Slf4j
public abstract class AbstractRSQLVisitor<A extends Enum<A> & RsqlQueryField> {

    private final Class<A> rsqlQueryFieldType;

    @Value
    protected class RsqlField {

        A enumValue;
        String[] subAttributes;

        private RsqlField(final A enumValue, final String[] subAttributes) {
            this.enumValue = enumValue;
            this.subAttributes = subAttributes;
        }
    }

    protected AbstractRSQLVisitor(final Class<A> rsqlQueryFieldType) {
        this.rsqlQueryFieldType = rsqlQueryFieldType;
    }

    protected RsqlField getRsqlField(final ComparisonNode node) {
        final String[] graph = node.getSelector().split(RsqlQueryField.SUB_ATTRIBUTE_SPLIT_REGEX);
        final String enumName = graph.length == 0 ? node.getSelector() : graph[0];
        log.debug("get field identifier by name {} of enum type {}", enumName, rsqlQueryFieldType);

        try {
            final A enumValue = Enum.valueOf(rsqlQueryFieldType, enumName.toUpperCase());
            final String[] subAttributes = enumValue.getSubAttributes(node.getSelector());

            // validate
            if (enumValue.isMap()) {
                // <enum>.<key>
                if (subAttributes.length != 2) {
                    throw new RSQLParameterUnsupportedFieldException(
                            "The syntax of the given map search parameter field {" + node.getSelector() + "} is wrong. Syntax is: <enum name>.<key name>");
                }
            } else {
                // sub entity need minimum 1 dot
                if (!enumValue.getSubEntityAttributes().isEmpty() && subAttributes.length < 2) {
                    throw createRSQLParameterUnsupportedException(node, null);
                }
            }

            // build property and validate sub attributes
            final StringBuilder fieldNameBuilder = new StringBuilder(enumValue.getJpaEntityFieldName());
            for (int i = 1; i < subAttributes.length; i++) {
                final String propertyField = getFormattedSubEntityAttribute(enumValue, subAttributes[i]);

                if (!enumValue.containsSubEntityAttribute(propertyField)) {
                    if (i != subAttributes.length - 1 || !enumValue.isMap()) {
                        throw createRSQLParameterUnsupportedException(node, null);
                    } // otherwise - the key of map is not in the sub entity attributes
                }

                fieldNameBuilder.append(RsqlQueryField.SUB_ATTRIBUTE_SEPARATOR).append(propertyField);
            }

            return new RsqlField(enumValue, enumValue.getSubAttributes(fieldNameBuilder.toString()));
        } catch (final IllegalArgumentException e) {
            throw createRSQLParameterUnsupportedException(node, e);
        }
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
        final List<String> expectedFieldList = Arrays.stream(rsqlQueryFieldType.getEnumConstants())
                .filter(enumField -> enumField.getSubEntityAttributes().isEmpty()).map(enumField -> {
                    final String enumFieldName = enumField.name().toLowerCase();
                    if (enumField.isMap()) {
                        return enumFieldName + RsqlQueryField.SUB_ATTRIBUTE_SEPARATOR + "keyName";
                    } else {
                        return enumFieldName;
                    }
                }).collect(Collectors.toList());

        final List<String> expectedSubFieldList = Arrays.stream(rsqlQueryFieldType.getEnumConstants())
                .filter(enumField -> !enumField.getSubEntityAttributes().isEmpty()).flatMap(enumField -> {
                    final List<String> subEntity = enumField
                            .getSubEntityAttributes().stream().map(fieldName -> enumField.name().toLowerCase()
                                    + RsqlQueryField.SUB_ATTRIBUTE_SEPARATOR + fieldName)
                            .toList();

                    return subEntity.stream();
                }).toList();
        expectedFieldList.addAll(expectedSubFieldList);
        return expectedFieldList;
    }
}