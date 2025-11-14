/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
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
 * Describing the fields of the Tag model which can be used in the REST API e.g. for sorting etc.
 * Additionally, here were added fields for Target in order filtering over target fields also.
 */
@Getter
public enum TargetTagFields implements QueryField {

    ID(TagFields.ID.getName()),
    NAME(TagFields.NAME.getName()),
    DESCRIPTION(TagFields.DESCRIPTION.getName()),
    COLOUR(TagFields.COLOUR.getName()),
    CREATEDAT("createdAt"),
    CREATEDBY("createdBy"),
    LASTMODIFIEDAT("lastModifiedAt"),
    LASTMODIFIEDBY("lastModifiedBy");

    private final String name;

    TargetTagFields(final String name) {
        this.name = name;
    }
}