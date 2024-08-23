/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others
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
 * Sort fields for TargetMetadata.
 */
@Getter
public enum TargetMetadataFields implements FieldNameProvider {

    KEY("key"),
    VALUE("value");

    private final String fieldName;

    TargetMetadataFields(final String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String identifierFieldName() {
        return KEY.getFieldName();
    }
}
