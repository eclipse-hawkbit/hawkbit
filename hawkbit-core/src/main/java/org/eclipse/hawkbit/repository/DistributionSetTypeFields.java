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
 * Describing the fields of the DistributionSetType model which can be used in
 * the REST API e.g. for sorting etc.
 *
 *
 *
 *
 */
public enum DistributionSetTypeFields implements FieldNameProvider {
    /**
     * The name field.
     */
    NAME("name"),
    /**
     * The description field.
     */
    DESCRIPTION("description"),

    /**
     * The type key field.
     */
    KEY("key"),

    /**
     * The id field.
     */
    ID("id");

    private final String fieldName;

    private DistributionSetTypeFields(final String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }
}
