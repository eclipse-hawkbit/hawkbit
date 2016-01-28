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
 * Describing the fields of the DistributionSet model which can be used in the
 * REST API e.g. for sorting etc.
 *
 *
 *
 *
 */
public enum DistributionSetFields implements FieldNameProvider {
    /**
     * The name field.
     */
    NAME("name"),
    /**
     * The description field.
     */
    DESCRIPTION("description"),
    /**
     * The version field.
     */
    VERSION("version"),
    /**
     * The complete field.
     */
    COMPLETE("complete"),
    /**
     * The id field.
     */
    ID("id"),

    /**
     * The tags field.
     */
    TAG("tags.name"),

    /**
     * The sw type key field.
     */
    TYPE("type.key"),

    /**
     * The metadata.
     */
    METADATA("metadata", "key", "value");

    private final String fieldName;
    private String keyFieldName;
    private String valueFieldName;

    private DistributionSetFields(final String fieldName) {
        this(fieldName, null, null);
    }

    private DistributionSetFields(final String fieldName, final String keyFieldName, final String valueFieldName) {
        this.fieldName = fieldName;
        this.keyFieldName = keyFieldName;
        this.valueFieldName = valueFieldName;
    }

    @Override
    public String getValueFieldName() {
        return valueFieldName;
    }

    @Override
    public String getKeyFieldName() {
        return keyFieldName;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

}
