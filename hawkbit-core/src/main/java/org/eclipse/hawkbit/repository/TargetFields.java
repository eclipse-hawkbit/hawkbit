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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

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
    ASSIGNEDDS(
            "assignedDistributionSet",
            DistributionSetFields.NAME.getJpaEntityFieldName(), DistributionSetFields.VERSION.getJpaEntityFieldName()),
    INSTALLEDDS(
            "installedDistributionSet",
            DistributionSetFields.NAME.getJpaEntityFieldName(), DistributionSetFields.VERSION.getJpaEntityFieldName()),
    TAG("tags", TagFields.NAME.getJpaEntityFieldName()),
    LASTCONTROLLERREQUESTAT("lastTargetQuery"),
    METADATA("metadata", new SimpleImmutableEntry<>("key", "value")),
    TARGETTYPE("targetType", TargetTypeFields.KEY.getJpaEntityFieldName(), TargetTypeFields.NAME.getJpaEntityFieldName());

    private final String jpaEntityFieldName;
    private final List<String> subEntityAttributes;
    private final Entry<String, String> subEntityMapTuple;

    TargetFields(final String jpaEntityFieldName) {
        this(jpaEntityFieldName, Collections.emptyList(), null);
    }

    TargetFields(final String jpaEntityFieldName, final String... subEntityAttributes) {
        this(jpaEntityFieldName, List.of(subEntityAttributes), null);
    }

    TargetFields(final String jpaEntityFieldName, final Entry<String, String> subEntityMapTuple) {
        this(jpaEntityFieldName, Collections.emptyList(), subEntityMapTuple);
    }

    TargetFields(final String jpaEntityFieldName, final List<String> subEntityAttributes, final Entry<String, String> subEntityMapTuple) {
        this.jpaEntityFieldName = jpaEntityFieldName;
        this.subEntityAttributes = subEntityAttributes;
        this.subEntityMapTuple = subEntityMapTuple;
    }

    @Override
    public Optional<Entry<String, String>> getSubEntityMapTuple() {
        return Optional.ofNullable(subEntityMapTuple);
    }

    @Override
    public boolean isMap() {
        return this == ATTRIBUTE || getSubEntityMapTuple().isPresent();
    }
}