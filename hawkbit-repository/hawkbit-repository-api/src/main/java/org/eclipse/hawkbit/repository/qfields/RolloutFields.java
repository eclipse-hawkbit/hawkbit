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
 * Describing the fields of the Rollout model which can be used in the REST API e.g. for sorting etc.
 */
@Getter
public enum RolloutFields implements QueryField {

    ID("id"),
    NAME("name"),
    DESCRIPTION("description"),
    STATUS("status"),
    CREATEDAT("createdAt"),
    CREATEDBY("createdBy"),
    LASTMODIFIEDAT("lastModifiedAt"),
    LASTMODIFIEDBY("lastModifiedBy"),
    DISTRIBUTIONSET(
            "distributionSet",
            DistributionSetFields.ID.getName(),
            DistributionSetFields.NAME.getName(), DistributionSetFields.VERSION.getName(),
            DistributionSetFields.TYPE.getName());

    private final String name;
    private final List<String> subEntityAttributes;

    RolloutFields(final String name, final String... subEntityAttributes) {
        this.name = name;
        this.subEntityAttributes = List.of(subEntityAttributes);
    }
}