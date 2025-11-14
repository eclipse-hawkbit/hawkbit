/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
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
 * Describing the fields of the TargetType model which can be used in the REST API
 */
@Getter
public enum TargetTypeFields implements QueryField {

    ID("id"),
    KEY("key"),
    NAME("name"),
    DESCRIPTION("description"),
    CREATEDAT("createdAt"),
    CREATEDBY("createdBy"),
    LASTMODIFIEDAT("lastModifiedAt"),
    LASTMODIFIEDBY("lastModifiedBy");

    private final String name;

    TargetTypeFields(final String name) {
        this.name = name;
    }
}