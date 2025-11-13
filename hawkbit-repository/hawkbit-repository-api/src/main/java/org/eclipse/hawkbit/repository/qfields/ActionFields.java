/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.qfields;

import java.util.List;

import lombok.Getter;

/**
 * Sort and search fields for actions.
 */
@Getter
public enum ActionFields implements QueryField {

    ID("id"),
    ACTIVE("active"),
    STATUS("status"),
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
}