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
import org.eclipse.hawkbit.ql.QueryField;

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
            TargetFields.ID.getName(), TargetFields.NAME.getName(),
            TargetFields.UPDATESTATUS.getName(), TargetFields.IPADDRESS.getName()),
    DISTRIBUTIONSET("distributionSet",
            DistributionSetFields.ID.getName(),
            DistributionSetFields.NAME.getName(), DistributionSetFields.VERSION.getName(),
            DistributionSetFields.TYPE.getName()),
    ROLLOUT("rollout", RolloutFields.ID.getName(), RolloutFields.NAME.getName()),
    ROLLOUTGROUP("rolloutGroup", RolloutGroupFields.ID.getName(), RolloutGroupFields.NAME.getName()),
    EXTERNALREF("externalRef");

    private final String name;
    private final List<String> subEntityAttributes;

    ActionFields(final String name, final String... subEntityAttributes) {
        this.name = name;
        this.subEntityAttributes = List.of(subEntityAttributes);
    }
}