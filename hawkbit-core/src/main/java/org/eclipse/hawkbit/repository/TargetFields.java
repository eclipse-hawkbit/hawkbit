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
 * Describing the fields of the Target model which can be used in the REST API
 * e.g. for sorting etc.
 */
@Getter
public enum TargetFields implements RsqlQueryField {

    ID("controllerId"),
    NAME("name"),
    DESCRIPTION("description"),
    CREATEDAT("createdAt"),
    LASTMODIFIEDAT("lastModifiedAt"),
    CONTROLLERID("controllerId"),
    UPDATESTATUS("updateStatus"),
    IPADDRESS("address"),
    ATTRIBUTE("controllerAttributes"),
    GROUP("group"),
    ASSIGNEDDS(
            "assignedDistributionSet",
            DistributionSetFields.NAME.getJpaEntityFieldName(), DistributionSetFields.VERSION.getJpaEntityFieldName()),
    INSTALLEDDS(
            "installedDistributionSet",
            DistributionSetFields.NAME.getJpaEntityFieldName(), DistributionSetFields.VERSION.getJpaEntityFieldName()),
    TAG("tags", TagFields.NAME.getJpaEntityFieldName()),
    LASTCONTROLLERREQUESTAT("lastTargetQuery"),
    METADATA("metadata"),
    TARGETTYPE("targetType", TargetTypeFields.KEY.getJpaEntityFieldName(), TargetTypeFields.NAME.getJpaEntityFieldName());

    private final String jpaEntityFieldName;
    private final List<String> subEntityAttributes;

    TargetFields(final String jpaEntityFieldName, final String... subEntityAttributes) {
        this.jpaEntityFieldName = jpaEntityFieldName;
        this.subEntityAttributes = List.of(subEntityAttributes);
    }

    @Override
    public boolean isMap() {
        return this == ATTRIBUTE || this == METADATA;
    }
}