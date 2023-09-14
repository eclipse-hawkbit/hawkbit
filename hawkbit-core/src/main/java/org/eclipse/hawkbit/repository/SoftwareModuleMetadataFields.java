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
 * Sort fields for SoftwareModuleMetadata.
 *
 *
 *
 *
 */
public enum SoftwareModuleMetadataFields implements FieldNameProvider {

    /**
     * The value field.
     */
    VALUE("value"),
    /**
     * The key field.
     */
    KEY("key"),

    /**
     * The target visible field.
     */
    TARGETVISIBLE("targetVisible");

    private final String fieldName;

    private SoftwareModuleMetadataFields(final String fieldName) {
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
