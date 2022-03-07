/**
 * Copyright (c) 2020 devolo AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.FieldNameProvider;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.jirutka.rsql.parser.ast.ComparisonNode;

public abstract class AbstractFieldNameRSQLVisitor<A extends Enum<A> & FieldNameProvider> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFieldNameRSQLVisitor.class);

    private final Class<A> fieldNameProvider;

    protected AbstractFieldNameRSQLVisitor(final Class<A> fieldNameProvider) {
        this.fieldNameProvider = fieldNameProvider;
    }

    protected A getFieldEnumByName(final ComparisonNode node) {
        String enumName = node.getSelector();
        final String[] graph = enumName.split("\\" + FieldNameProvider.SUB_ATTRIBUTE_SEPARATOR);
        if (graph.length != 0) {
            enumName = graph[0];
        }
        LOGGER.debug("get field identifier by name {} of enum type {}", enumName, fieldNameProvider);
        try {
            return Enum.valueOf(fieldNameProvider, enumName.toUpperCase());
        } catch (final IllegalArgumentException e) {
            throw createRSQLParameterUnsupportedException(node, e);
        }
    }

    protected String getAndValidatePropertyFieldName(final A propertyEnum, final ComparisonNode node) {

        final String[] graph = propertyEnum.getSubAttributes(node.getSelector());

        validateMapParameter(propertyEnum, node, graph);

        // sub entity need minimum 1 dot
        if (!propertyEnum.getSubEntityAttributes().isEmpty() && graph.length < 2) {
            throw createRSQLParameterUnsupportedException(node, null);
        }

        final StringBuilder fieldNameBuilder = new StringBuilder(propertyEnum.getFieldName());

        for (int i = 1; i < graph.length; i++) {

            final String propertyField = graph[i];
            fieldNameBuilder.append(FieldNameProvider.SUB_ATTRIBUTE_SEPARATOR).append(propertyField);

            // the key of map is not in the graph
            if (propertyEnum.isMap() && graph.length == (i + 1)) {
                continue;
            }

            if (!propertyEnum.containsSubEntityAttribute(propertyField)) {
                throw createRSQLParameterUnsupportedException(node, null);
            }
        }

        return fieldNameBuilder.toString();
    }

    protected void validateMapParameter(final A propertyEnum, final ComparisonNode node, final String[] graph) {
        if (!propertyEnum.isMap()) {
            return;

        }

        if (!propertyEnum.getSubEntityAttributes().isEmpty()) {
            throw new UnsupportedOperationException(
                    "Currently subentity attributes for maps are not supported, alternatively you could use the key/value tuple, defined by SimpleImmutableEntry class");
        }

        // enum.key
        final int minAttributeForMap = 2;
        if (graph.length != minAttributeForMap) {
            throw new RSQLParameterUnsupportedFieldException("The syntax of the given map search parameter field {"
                    + node.getSelector() + "} is wrong. Syntax is: fieldname.keyname", new Exception());
        }
    }

    /**
     * @param node
     *            current processing node
     * @param rootException
     *            in case there is a cause otherwise {@code null}
     * @return Exception with prepared message extracted from the comparison node.
     */
    protected RSQLParameterUnsupportedFieldException createRSQLParameterUnsupportedException(
            @NotNull final ComparisonNode node,
            final Exception rootException) {
        return new RSQLParameterUnsupportedFieldException(String.format(
                "The given search parameter field {%s} does not exist, must be one of the following fields %s",
                node.getSelector(), getExpectedFieldList()), rootException);
    }

    private List<String> getExpectedFieldList() {
        final List<String> expectedFieldList = Arrays.stream(fieldNameProvider.getEnumConstants())
                .filter(enumField -> enumField.getSubEntityAttributes().isEmpty()).map(enumField -> {
                    final String enumFieldName = enumField.name().toLowerCase();

                    if (enumField.isMap()) {
                        return enumFieldName + FieldNameProvider.SUB_ATTRIBUTE_SEPARATOR + "keyName";
                    }

                    return enumFieldName;
                }).collect(Collectors.toList());

        final List<String> expectedSubFieldList = Arrays.stream(fieldNameProvider.getEnumConstants())
                .filter(enumField -> !enumField.getSubEntityAttributes().isEmpty()).flatMap(enumField -> {
                    final List<String> subEntity = enumField
                            .getSubEntityAttributes().stream().map(fieldName -> enumField.name().toLowerCase()
                                    + FieldNameProvider.SUB_ATTRIBUTE_SEPARATOR + fieldName)
                            .collect(Collectors.toList());

                    return subEntity.stream();
                }).collect(Collectors.toList());
        expectedFieldList.addAll(expectedSubFieldList);
        return expectedFieldList;
    }
}
