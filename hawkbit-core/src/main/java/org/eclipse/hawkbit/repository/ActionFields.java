/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Sort fields for {@link ActionRest}.
 */
public enum ActionFields implements FieldNameProvider, FieldValueConverter<ActionFields> {

    /**
     * The status field.
     */
    STATUS("active"),

    /**
     * The detailed action status
     */
    DETAILSTATUS("status"),
    
    /**
     * The last action status code
     */
    LASTSTATUSCODE("lastActionStatusCode"),

    /**
     * The id field.
     */
    ID("id"),

    /**
     * The weight field.
     */
    WEIGHT("weight"),

    /**
     * The target field
     */
    TARGET("target", TargetFields.ID.getFieldName(), TargetFields.NAME.getFieldName(),
            TargetFields.UPDATESTATUS.getFieldName(), TargetFields.IPADDRESS.getFieldName()),

    /**
     * The distribution set field
     */
    DISTRIBUTIONSET("distributionSet", DistributionSetFields.ID.getFieldName(),
            DistributionSetFields.NAME.getFieldName(), DistributionSetFields.VERSION.getFieldName(),
            DistributionSetFields.TYPE.getFieldName()),

    /**
     * The rollout field
     */
    ROLLOUT("rollout", RolloutFields.ID.getFieldName(), RolloutFields.NAME.getFieldName()),

    /**
     * The rollout field
     */
    ROLLOUTGROUP("rolloutGroup", RolloutGroupFields.ID.getFieldName(), RolloutGroupFields.NAME.getFieldName());

    private static final String ACTIVE = "pending";
    private static final String INACTIVE = "finished";

    private final String fieldName;

    private List<String> subEntityAttributes;

    private ActionFields(final String fieldName) {
        this.fieldName = fieldName;
        this.subEntityAttributes = Collections.emptyList();
    }

    private ActionFields(final String fieldName, final String... subEntityAttributes) {
        this.fieldName = fieldName;
        this.subEntityAttributes = Arrays.asList(subEntityAttributes);
    }

    @Override
    public List<String> getSubEntityAttributes() {
        return subEntityAttributes;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public Object convertValue(final ActionFields e, final String value) {
        if (STATUS == e) {
            return convertStatusValue(value);
        }
        return value;
    }

    @Override
    public String[] possibleValues(final ActionFields e) {
        if (STATUS == e) {
            return new String[] { ACTIVE, INACTIVE };
        }
        return new String[0];
    }

    private static Object convertStatusValue(final String value) {
        final String trimmedValue = value.trim();
        if (trimmedValue.equalsIgnoreCase(ACTIVE)) {
            return 1;
        } else if (trimmedValue.equalsIgnoreCase(INACTIVE)) {
            return 0;
        } else {
            return null;
        }
    }

}
