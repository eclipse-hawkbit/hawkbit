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
 * Sort and search fields for actions.
 */
@Getter
public enum ActionFields implements QueryField {

    ID("id"),
    @Deprecated(since = "0.10.0", forRemoval = true) // use ACTIVE
    STATUS("active"), // true if status is "pending", false if "finished", after removal, will deprecate DETAILSTATUS too and replace with STATUS
    ACTIVE("active"), // true if st
    DETAILSTATUS("status"), // real status
    LASTSTATUSCODE("lastActionStatusCode"),
    CREATEDAT("createdAt"),
    CREATEDBY("createdBy"),
    LASTMODIFIEDAT("lastModifiedAt"),
    LASTMODIFIEDBY("lastModifiedBy"),
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

    private final String jpaEntityFieldName;
    private final List<String> subEntityAttributes;

    ActionFields(final String jpaEntityFieldName, final String... subEntityAttributes) {
        this.jpaEntityFieldName = jpaEntityFieldName;
        this.subEntityAttributes = List.of(subEntityAttributes);
    }

    /**
     * @deprecated since 0.10.0 - use {@link #ACTIVE} instead of {@link #STATUS}
     */
    @Deprecated(since = "0.10.0", forRemoval = true) // remove together with STATUS (with active meaning)
    public static Object convertStatusValue(final String value) {
        final String trimmedValue = value.trim();
        if (trimmedValue.equalsIgnoreCase("pending")) {
            return true;
        } else if (trimmedValue.equalsIgnoreCase("finished")) {
            return false;
        } else {
            throw new IllegalArgumentException("field 'status' must be one of the following values {pending, finished}");
        }
    }
}