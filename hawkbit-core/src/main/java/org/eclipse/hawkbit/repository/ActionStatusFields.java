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
 * Sort and search fields for action status.
 */
@Getter
public enum ActionStatusFields implements RsqlQueryField {

    ID("id"),
    CREATEDAT("createdAt"),
    CREATEDBY("createdBy");

    private final String jpaEntityFieldName;

    ActionStatusFields(final String jpaEntityFieldName) {
        this.jpaEntityFieldName = jpaEntityFieldName;
    }
}