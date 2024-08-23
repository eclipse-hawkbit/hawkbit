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
 * Describing the fields of the DistributionSet model which can be used in the
 * REST API e.g. for sorting etc.
 */
@Getter
public enum DistributionSetFields implements FieldNameProvider {

    ID("id"),
    TYPE("type.key"),
    NAME("name"),
    DESCRIPTION("description"),
    CREATEDAT("createdAt"),
    LASTMODIFIEDAT("lastModifiedAt"),
    VERSION("version"),
    COMPLETE("complete"),
    MODULE("modules", SoftwareModuleFields.ID.getFieldName(), SoftwareModuleFields.NAME.getFieldName()),
    TAG("tags.name"),
    METADATA("metadata", new SimpleImmutableEntry<>("key", "value")),
    VALID("valid");

    private final String fieldName;
    private final Entry<String, String> subEntityMapTuple;
    private final List<String> subEntityAttributes;

    DistributionSetFields(final String fieldName) {
        this(fieldName, null, Collections.emptyList());
    }

    DistributionSetFields(final String fieldName, final String... subEntityAttributes) {
        this(fieldName, null, List.of(subEntityAttributes));
    }

    DistributionSetFields(final String fieldName, final Entry<String, String> subEntityMapTuple) {
        this(fieldName, subEntityMapTuple, Collections.emptyList());
    }

    DistributionSetFields(final String fieldName, final Entry<String, String> subEntityMapTuple, List<String> subEntityAttributes) {
        this.fieldName = fieldName;
        this.subEntityMapTuple = subEntityMapTuple;
        this.subEntityAttributes = subEntityAttributes;
    }

    @Override
    public Optional<Entry<String, String>> getSubEntityMapTuple() {
        return Optional.ofNullable(subEntityMapTuple);
    }
}