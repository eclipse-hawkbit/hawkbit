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

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * Describing the fields of the Target model which can be used in the REST API e.g. for sorting etc.
 */
@Getter
public enum TargetFilterQueryFields implements FieldNameProvider {

    ID("id"),
    NAME("name"),
    AUTOASSIGNDISTRIBUTIONSET("autoAssignDistributionSet", "name", "version");

    private final String fieldName;
    private List<String> subEntityAttributes;

    TargetFilterQueryFields(final String fieldName) {
        this(fieldName, Collections.emptyList());
    }

    TargetFilterQueryFields(final String fieldName, final String... subEntityAttribues) {
        this(fieldName, List.of(subEntityAttribues));
    }

    TargetFilterQueryFields(final String fieldName, final List<String> subEntityAttribues) {
        this.fieldName = fieldName;
        this.subEntityAttributes = subEntityAttribues;
    }
}
