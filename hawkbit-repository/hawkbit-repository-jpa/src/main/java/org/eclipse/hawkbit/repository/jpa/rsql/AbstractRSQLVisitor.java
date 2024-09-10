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
import org.springframework.util.ObjectUtils;

@Slf4j
public abstract class AbstractRSQLVisitor<A extends Enum<A> & RsqlQueryField> {

    private final Class<A> rsqlQueryFieldType;

    @Value
    protected class QuertPath {

        A enumValue;
        String[] jpaPath;

        private QuertPath(final A enumValue, final String[] jpaPath) {
            this.enumValue = enumValue;
            this.jpaPath = jpaPath;
        }
    }

    protected AbstractRSQLVisitor(final Class<A> rsqlQueryFieldType) {
        this.rsqlQueryFieldType = rsqlQueryFieldType;
    }

    protected QuertPath getQuertPath(final ComparisonNode node) {
        final String[] path = node.getSelector().split(RsqlQueryField.SUB_ATTRIBUTE_SPLIT_REGEX);
        if (path.length == 0) {
            throw createRSQLParameterUnsupportedException(node, null);
        }

        final String enumName = path[0].toUpperCase();
        log.debug("get field identifier by name {} of enum type {}", enumName, rsqlQueryFieldType);

        try {
            final A enumValue = Enum.valueOf(rsqlQueryFieldType, enumName);
            String[] split = getSplit(enumValue, node.getSelector());

            // validate
            if (enumValue.isMap()) {
                // <enum>.<key>
                if (split.length != 2) {
                    throw new RSQLParameterUnsupportedFieldException(
                            "The syntax of the given map search parameter field {" + node.getSelector() + "} is wrong. Syntax is: <enum name>.<key name>");
                }
            } else {
                // sub entity need minimum 1 dot
                if (!enumValue.getSubEntityAttributes().isEmpty() && split.length < 2) {
                    if (enumValue.getSubEntityAttributes().size() == 1) { // single sub attribute - so default
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

                if (!enumValue.containsSubEntityAttribute(split[i])) {
                    if (i != split.length - 1 || !enumValue.isMap()) {
                        throw createRSQLParameterUnsupportedException(node, null);
                    } // otherwise - the key of map is not in the sub entity attributes
                }
            }

            return new QuertPath(enumValue, split);
        } catch (final IllegalArgumentException e) {
            throw createRSQLParameterUnsupportedException(node, e);
        }
    }

    /**
     * Returns the sub attributes
     *
     * @param rsqlFieldName the given field
     * @return array consisting of sub attributes
     */
    private String[] getSplit(final A enumValue, final String rsqlFieldName) {
        if (enumValue.isMap()) {
            final String[] subAttributes = rsqlFieldName.split(RsqlQueryField.SUB_ATTRIBUTE_SPLIT_REGEX, 2);
            // [0] field name | [1] key name (could miss, e.g. for target attributes)
            final String mapKeyName = subAttributes.length == 2 ? subAttributes[1] : null;
            return ObjectUtils.isEmpty(mapKeyName) ?
                    new String[] { enumValue.getJpaEntityFieldName() } :
                    new String[] { enumValue.getJpaEntityFieldName(), mapKeyName };
        } else {
            return rsqlFieldName.split(RsqlQueryField.SUB_ATTRIBUTE_SPLIT_REGEX);
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