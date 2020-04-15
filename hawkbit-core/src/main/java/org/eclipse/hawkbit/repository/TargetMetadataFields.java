/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

/**
 * Sort fields for TargetMetadata.
 *
 */
public enum TargetMetadataFields implements FieldNameProvider {

    /**
     * The value field.
     */
    VALUE("value"),
    /**
     * The key field.
     */
    KEY("key");

    private final String fieldName;

    private TargetMetadataFields(final String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String identifierFieldName() {
        return KEY.getFieldName();
    }
}
