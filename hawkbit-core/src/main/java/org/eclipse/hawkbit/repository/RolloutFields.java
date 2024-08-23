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

import java.util.Collections;
import java.util.List;

/**
 * Describing the fields of the Rollout model which can be used in the REST API e.g. for sorting etc.
 */
@Getter
public enum RolloutFields implements FieldNameProvider {

    ID("id"),
    NAME("name"),
    DESCRIPTION("description"),
    STATUS("status"),
    DISTRIBUTIONSET("distributionSet", DistributionSetFields.ID.getFieldName(),
            DistributionSetFields.NAME.getFieldName(), DistributionSetFields.VERSION.getFieldName(),
            DistributionSetFields.TYPE.getFieldName());

    private final String fieldName;
    private final List<String> subEntityAttributes;

    RolloutFields(final String fieldName) {
        this.fieldName = fieldName;
        this.subEntityAttributes = Collections.emptyList();
    }

    RolloutFields(final String fieldName, final String... subEntityAttributes) {
        this.fieldName = fieldName;
        this.subEntityAttributes = List.of(subEntityAttributes);
    }
}