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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Describing the fields of the Rollout model which can be used in the REST API
 * e.g. for sorting etc.
 *
 */
public enum RolloutFields implements FieldNameProvider {
    /**
     * The name field.
     */
    NAME("name"),
    /**
     * The description field.
     */
    DESCRIPTION("description"),
    /**
     * The id field.
     */
    ID("id"),
    /**
     * The status field.
     */
    STATUS("status"),
    /**
     * The Distribution set field.
     */
    DISTRIBUTIONSET("distributionSet", DistributionSetFields.ID.getFieldName(),
            DistributionSetFields.NAME.getFieldName(), DistributionSetFields.VERSION.getFieldName(),
            DistributionSetFields.TYPE.getFieldName());

    private final String fieldName;

    private final List<String> subEntityAttributes;

    private RolloutFields(final String fieldName) {
        this.fieldName = fieldName;
        this.subEntityAttributes = Collections.emptyList();
    }

    private RolloutFields(final String fieldName, final String... subEntityAttributes) {
        this.fieldName = fieldName;
        this.subEntityAttributes = Arrays.asList(subEntityAttributes);
    }

    @Override
    public List<String> getSubEntityAttributes() {
        return Collections.unmodifiableList(subEntityAttributes);
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }
}
