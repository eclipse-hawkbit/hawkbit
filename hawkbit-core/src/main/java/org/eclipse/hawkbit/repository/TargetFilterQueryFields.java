/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Describing the fields of the Target model which can be used in the REST API
 * e.g. for sorting etc.
 *
 */
public enum TargetFilterQueryFields implements FieldNameProvider {

    /**
     * The id field.
     */
    ID("id"),

    /**
     * The name field.
     */
    NAME("name"),

    /**
     * Distribution set for auto-assignment.
     */
    AUTOASSIGNDISTRIBUTIONSET("autoAssignDistributionSet", "name", "version");

    private final String fieldName;
    private List<String> subEntityAttributes;

    private TargetFilterQueryFields(final String fieldName) {
        this(fieldName, Collections.emptyList());
    }

    private TargetFilterQueryFields(final String fieldName, final String... subEntityAttribues) {
        this(fieldName, Arrays.asList(subEntityAttribues));
    }

    private TargetFilterQueryFields(final String fieldName, final List<String> subEntityAttribues) {
        this.fieldName = fieldName;
        this.subEntityAttributes = subEntityAttribues;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public List<String> getSubEntityAttributes() {
        return subEntityAttributes;
    }
}
