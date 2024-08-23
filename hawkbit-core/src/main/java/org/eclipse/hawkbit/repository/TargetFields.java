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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * Describing the fields of the Target model which can be used in the REST API
 * e.g. for sorting etc.
 */
@Getter
public enum TargetFields implements FieldNameProvider {

    ID("controllerId"),
    NAME("name"),
    DESCRIPTION("description"),
    CREATEDAT("createdAt"),
    LASTMODIFIEDAT("lastModifiedAt"),
    CONTROLLERID("controllerId"),
    UPDATESTATUS("updateStatus"),
    IPADDRESS("address"),
    ATTRIBUTE("controllerAttributes"),
    ASSIGNEDDS("assignedDistributionSet", "name", "version"),
    INSTALLEDDS("installedDistributionSet", "name", "version"),
    TAG("tags.name"),
    LASTCONTROLLERREQUESTAT("lastTargetQuery"),
    METADATA("metadata", new SimpleImmutableEntry<>("key", "value")),
    TARGETTYPE("targetType", TargetTypeFields.KEY.getFieldName(), TargetTypeFields.NAME.getFieldName());

    private final String fieldName;
    private final List<String> subEntityAttributes;
    private final Entry<String, String> subEntityMapTuple;

    TargetFields(final String fieldName) {
        this(fieldName, Collections.emptyList(), null);
    }

    TargetFields(final String fieldName, final String... subEntityAttributes) {
        this(fieldName, List.of(subEntityAttributes), null);
    }

    TargetFields(final String fieldName, final Entry<String, String> subEntityMapTuple) {
        this(fieldName, Collections.emptyList(), subEntityMapTuple);
    }

    TargetFields(final String fieldName, final List<String> subEntityAttributes, final Entry<String, String> subEntityMapTuple) {
        this.fieldName = fieldName;
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