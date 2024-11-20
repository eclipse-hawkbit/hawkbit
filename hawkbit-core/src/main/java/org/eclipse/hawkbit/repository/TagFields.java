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

/**
 * Describing the fields of the Tag model which can be used in the REST API e.g. for sorting etc.
 */
@Getter
public enum TagFields implements RsqlQueryField {

    ID("id"),
    NAME("name"),
    DESCRIPTION("description"),
    COLOUR("colour");

    private final String jpaEntityFieldName;

    TagFields(final String jpaEntityFieldName) {
        this.jpaEntityFieldName = jpaEntityFieldName;
    }
}