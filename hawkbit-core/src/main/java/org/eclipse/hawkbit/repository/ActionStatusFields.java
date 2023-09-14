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
 * Sort fields for {@link ActionStatusRest}.
 *
 *
 *
 *
 */
public enum ActionStatusFields implements FieldNameProvider {

    /**
     * The id field.
     */
    ID("id"),

    /**
     * The reportedAt field.
     */
    REPORTEDAT("createdAt");

    private final String fieldName;

    private ActionStatusFields(final String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }
}
