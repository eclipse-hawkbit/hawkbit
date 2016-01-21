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
 * Sort fields for DistributionSetMetadata.
 *
 *
 *
 *
 */
public enum DistributionSetMetadataFields implements FieldNameProvider {

    /**
     * The value field.
     */
    VALUE("value"),
    /**
     * The key field.
     */
    KEY("key");

    private final String fieldName;

    private DistributionSetMetadataFields(final String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }
}
