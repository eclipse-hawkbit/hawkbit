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

import lombok.Getter;
import org.eclipse.hawkbit.ql.QueryField;

/**
 * Describing the fields of the SoftwareModuleType model which can be used in the REST API e.g. for sorting etc.
 */
@Getter
public enum SoftwareModuleTypeFields implements QueryField {

    ID("id"),
    KEY("key"),
    NAME("name"),
    DESCRIPTION("description"),
    MAXASSIGNMENTS("maxAssignments"),
    CREATEDAT("createdAt"),
    CREATEDBY("createdBy"),
    LASTMODIFIEDAT("lastModifiedAt"),
    LASTMODIFIEDBY("lastModifiedBy");

    private final String name;

    SoftwareModuleTypeFields(final String name) {
        this.name = name;
    }
}
