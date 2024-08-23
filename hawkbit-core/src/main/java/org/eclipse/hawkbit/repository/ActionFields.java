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
 * Sort and search fields for actions.
 */
@Getter
public enum ActionFields implements FieldNameProvider, FieldValueConverter<ActionFields> {

    ID("id"),
    STATUS("active"),
    DETAILSTATUS("status"),
    LASTSTATUSCODE("lastActionStatusCode"),
    WEIGHT("weight"),
    TARGET("target",
            TargetFields.ID.getFieldName(), TargetFields.NAME.getFieldName(),
            TargetFields.UPDATESTATUS.getFieldName(), TargetFields.IPADDRESS.getFieldName()),
    DISTRIBUTIONSET("distributionSet",
            DistributionSetFields.ID.getFieldName(),
            DistributionSetFields.NAME.getFieldName(), DistributionSetFields.VERSION.getFieldName(),
            DistributionSetFields.TYPE.getFieldName()),
    ROLLOUT("rollout", RolloutFields.ID.getFieldName(), RolloutFields.NAME.getFieldName()),
    ROLLOUTGROUP("rolloutGroup", RolloutGroupFields.ID.getFieldName(), RolloutGroupFields.NAME.getFieldName()),
    EXTERNALREF("externalRef");

    private static final String ACTIVE = "pending";
    private static final String INACTIVE = "finished";

    private final String fieldName;
    private final List<String> subEntityAttributes;

    ActionFields(final String fieldName) {
        this.fieldName = fieldName;
        this.subEntityAttributes = Collections.emptyList();
    }

    ActionFields(final String fieldName, final String... subEntityAttributes) {
        this.fieldName = fieldName;
        this.subEntityAttributes = List.of(subEntityAttributes);
    }

    @Override
    public Object convertValue(final ActionFields e, final String value) {
        return STATUS == e ? convertStatusValue(value) : value;
    }

    @Override
    public String[] possibleValues(final ActionFields e) {
        return STATUS == e ? new String[] { ACTIVE, INACTIVE } : new String[0];
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