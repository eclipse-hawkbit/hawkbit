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
 * Sort fields for {@link ActionRest}.
 *
 *
 *
 *
 */
public enum ActionFields implements FieldNameProvider, FieldValueConverter<ActionFields> {

    /**
     * The status field.
     */
    STATUS("active"),
    /**
     * The id field.
     */
    ID("id");

    private static final String ACTIVE = "pending";
    private static final String INACTIVE = "finished";

    private final String fieldName;

    private ActionFields(final String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public Object convertValue(final ActionFields e, final String value) {
        switch (e) {
        case STATUS:
            return convertStatusValue(value);

        default:
            return value;
        }
    }

    private Object convertStatusValue(final String value) {
        final String trimmedValue = value.trim();
        if (trimmedValue.equalsIgnoreCase(ACTIVE)) {
            return 1;
        } else if (trimmedValue.equalsIgnoreCase(INACTIVE)) {
            return 0;
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.rest.resource.model.FieldValueConverter#
     * possibleValues(java.lang.Enum)
     */
    @Override
    public String[] possibleValues(final ActionFields e) {
        switch (e) {
        case STATUS:
            return new String[] { ACTIVE, INACTIVE };
        default:
            return new String[0];
        }
    }
}
