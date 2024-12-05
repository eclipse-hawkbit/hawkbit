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

import java.util.Collections;
import java.util.List;

import lombok.Getter;

/**
 * Sort and search fields for actions.
 */
@Getter
public enum ActionFields implements RsqlQueryField, FieldValueConverter<ActionFields> {

    ID("id"),
    STATUS("active"),
    DETAILSTATUS("status"),
    LASTSTATUSCODE("lastActionStatusCode"),
    WEIGHT("weight"),
    TARGET("target",
            TargetFields.ID.getJpaEntityFieldName(), TargetFields.NAME.getJpaEntityFieldName(),
            TargetFields.UPDATESTATUS.getJpaEntityFieldName(), TargetFields.IPADDRESS.getJpaEntityFieldName()),
    DISTRIBUTIONSET("distributionSet",
            DistributionSetFields.ID.getJpaEntityFieldName(),
            DistributionSetFields.NAME.getJpaEntityFieldName(), DistributionSetFields.VERSION.getJpaEntityFieldName(),
            DistributionSetFields.TYPE.getJpaEntityFieldName()),
    ROLLOUT("rollout", RolloutFields.ID.getJpaEntityFieldName(), RolloutFields.NAME.getJpaEntityFieldName()),
    ROLLOUTGROUP("rolloutGroup", RolloutGroupFields.ID.getJpaEntityFieldName(), RolloutGroupFields.NAME.getJpaEntityFieldName()),
    EXTERNALREF("externalRef");

    private static final String ACTIVE = "pending";
    private static final String INACTIVE = "finished";

    private final String jpaEntityFieldName;
    private final List<String> subEntityAttributes;

    ActionFields(final String jpaEntityFieldName) {
        this.jpaEntityFieldName = jpaEntityFieldName;
        this.subEntityAttributes = Collections.emptyList();
    }

    ActionFields(final String jpaEntityFieldName, final String... subEntityAttributes) {
        this.jpaEntityFieldName = jpaEntityFieldName;
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