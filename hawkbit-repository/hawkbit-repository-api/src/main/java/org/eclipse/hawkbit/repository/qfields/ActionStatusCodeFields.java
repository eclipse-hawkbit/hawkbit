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

import lombok.Getter;
import org.eclipse.hawkbit.ql.QueryField;

/**
 * Describing the fields of the ActionStatusCode model which can be used in the REST API.
 */
@Getter
public enum ActionStatusCodeFields implements QueryField {

    CODE("code"),
    NAME("name"),
    SEVERITY("severity");

    private final String name;

    ActionStatusCodeFields(final String name) {
        this.name = name;
    }
}
