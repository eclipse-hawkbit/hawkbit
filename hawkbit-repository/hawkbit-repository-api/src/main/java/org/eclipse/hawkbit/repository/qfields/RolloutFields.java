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
            DistributionSetFields.ID.getJpaEntityFieldName(),
            DistributionSetFields.NAME.getJpaEntityFieldName(), DistributionSetFields.VERSION.getJpaEntityFieldName(),
            DistributionSetFields.TYPE.getJpaEntityFieldName());

    private final String jpaEntityFieldName;
    private final List<String> subEntityAttributes;

    RolloutFields(final String jpaEntityFieldName, final String... subEntityAttributes) {
        this.jpaEntityFieldName = jpaEntityFieldName;
        this.subEntityAttributes = List.of(subEntityAttributes);
    }
}