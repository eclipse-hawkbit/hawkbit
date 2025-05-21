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

import java.util.List;

import lombok.Getter;

/**
 * Describing the fields of the SoftwareModule model which can be used in the REST API e.g. for sorting etc.
 */
@Getter
public enum SoftwareModuleFields implements RsqlQueryField {

    ID("id"),
    TYPE("type", "key"),
    NAME("name"),
    DESCRIPTION("description"),
    VERSION("version"),
    METADATA("metadata");

    private final String jpaEntityFieldName;
    private final List<String> subEntityAttributes;

    SoftwareModuleFields(final String jpaEntityFieldName, final String... subEntityAttributes) {
        this.jpaEntityFieldName = jpaEntityFieldName;
        this.subEntityAttributes = List.of(subEntityAttributes);
    }

    @Override
    public boolean isMap() {
        return this == METADATA;
    }
}