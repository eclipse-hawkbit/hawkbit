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
public enum DistributionSetFields implements RsqlQueryField {

    ID("id"),
    TYPE("type", "key"),
    NAME("name"),
    DESCRIPTION("description"),
    CREATEDAT("createdAt"),
    LASTMODIFIEDAT("lastModifiedAt"),
    VERSION("version"),
    COMPLETE("complete"),
    MODULE("modules", SoftwareModuleFields.ID.getJpaEntityFieldName(), SoftwareModuleFields.NAME.getJpaEntityFieldName()),
    TAG("tags", "name"),
    METADATA("metadata", new SimpleImmutableEntry<>("key", "value")),
    VALID("valid");

    private final String jpaEntityFieldName;
    private final List<String> subEntityAttributes;
    private final Entry<String, String> subEntityMapTuple;

    DistributionSetFields(final String jpaEntityFieldName) {
        this(jpaEntityFieldName, Collections.emptyList(), null);
    }

    DistributionSetFields(final String jpaEntityFieldName, final String... subEntityAttributes) {
        this(jpaEntityFieldName, List.of(subEntityAttributes), null);
    }

    DistributionSetFields(final String jpaEntityFieldName, final Entry<String, String> subEntityMapTuple) {
        this(jpaEntityFieldName, Collections.emptyList(), subEntityMapTuple);
    }

    DistributionSetFields(final String jpaEntityFieldName, List<String> subEntityAttributes, final Entry<String, String> subEntityMapTuple) {
        this.jpaEntityFieldName = jpaEntityFieldName;
        this.subEntityMapTuple = subEntityMapTuple;
        this.subEntityAttributes = subEntityAttributes;
    }

    @Override
    public Optional<Entry<String, String>> getSubEntityMapTuple() {
        return Optional.ofNullable(subEntityMapTuple);
    }
}