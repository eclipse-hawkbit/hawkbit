/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

@Getter
public enum AutoAssignmentFields implements QueryField {

    ID("id"),
    NAME("name"),
    CREATEDAT("createdAt"),
    CREATEDBY("createdBy"),
    LASTMODIFIEDAT("lastModifiedAt"),
    LASTMODIFIEDBY("lastModifiedBy"),
    DISTRIBUTIONSET("distributionSet", "name", "version");

    private final String name;
    private final List<String> subEntityAttributes;

    AutoAssignmentFields(final String name) {
        this(name, Collections.emptyList());
    }

    AutoAssignmentFields(final String name, final String... subEntityAttribues) {
        this(name, List.of(subEntityAttribues));
    }

    AutoAssignmentFields(final String name, final List<String> subEntityAttribues) {
        this.name = name;
        this.subEntityAttributes = subEntityAttribues;
    }
}
