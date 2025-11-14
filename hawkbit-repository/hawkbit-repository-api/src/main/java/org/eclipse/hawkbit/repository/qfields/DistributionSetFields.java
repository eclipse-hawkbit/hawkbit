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

import java.util.List;

import lombok.Getter;
import org.eclipse.hawkbit.ql.QueryField;

/**
 * Describing the fields of the DistributionSet model which can be used in the
 * REST API e.g. for sorting etc.
 */
@Getter
public enum DistributionSetFields implements QueryField {

    ID("id"),
    TYPE("type",
            DistributionSetTypeFields.ID.getName(),
            DistributionSetTypeFields.KEY.getName(),
            DistributionSetTypeFields.NAME.getName()),
    NAME("name"),
    DESCRIPTION("description"),
    CREATEDAT("createdAt"),
    CREATEDBY("createdBy"),
    LASTMODIFIEDAT("lastModifiedAt"),
    LASTMODIFIEDBY("lastModifiedBy"),
    VERSION("version"),
    MODULE("modules", SoftwareModuleFields.ID.getName(), SoftwareModuleFields.NAME.getName()),
    TAG("tags", "name"),
    METADATA("metadata"),
    VALID("valid");

    private final String name;
    private final List<String> subEntityAttributes;

    DistributionSetFields(final String name, final String... subEntityAttributes) {
        this.name = name;
        this.subEntityAttributes = List.of(subEntityAttributes);
    }

    @Override
    public String getDefaultSubEntityAttribute() {
        return this == TYPE ? DistributionSetTypeFields.KEY.getName() : QueryField.super.getDefaultSubEntityAttribute();
    }

    @Override
    public boolean isMap() {
        return this == METADATA;
    }
}