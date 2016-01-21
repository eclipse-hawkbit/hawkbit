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
