/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.qfields;

import java.util.Collections;
import java.util.List;

import lombok.Getter;
import org.eclipse.hawkbit.ql.QueryField;

/**
 * Describing the fields of the Target model which can be used in the REST API e.g. for sorting etc.
 */
@Getter
public enum TargetFilterQueryFields implements QueryField {

    ID("id"),
    NAME("name"),
    CREATEDAT("createdAt"),
    CREATEDBY("createdBy"),
    LASTMODIFIEDAT("lastModifiedAt"),
    LASTMODIFIEDBY("lastModifiedBy"),
    AUTOASSIGNDISTRIBUTIONSET("autoAssignDistributionSet", "name", "version");

    private final String name;
    private final List<String> subEntityAttributes;

    TargetFilterQueryFields(final String name) {
        this(name, Collections.emptyList());
    }

    TargetFilterQueryFields(final String name, final String... subEntityAttribues) {
        this(name, List.of(subEntityAttribues));
    }

    TargetFilterQueryFields(final String name, final List<String> subEntityAttribues) {
        this.name = name;
        this.subEntityAttributes = subEntityAttribues;
    }
}