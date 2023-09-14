/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
