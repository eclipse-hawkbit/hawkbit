/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * Describing the fields of the DistributionSet model which can be used in the
 * REST API e.g. for sorting etc.
 *
 *
 *
 *
 */
public enum DistributionSetFields implements FieldNameProvider {
    /**
     * The name field.
     */
    NAME("name"),
    /**
     * The description field.
     */
    DESCRIPTION("description"),
    /**
     * The createdAt field.
     */
    CREATEDAT("createdAt"),
    /**
     * The lastModifiedAt field.
     */
    LASTMODIFIEDAT("lastModifiedAt"),
    /**
     * The version field.
     */
    VERSION("version"),
    /**
     * The complete field.
     */
    COMPLETE("complete"),
    /**
     * The id field.
     */
    ID("id"),
    /**
     * The module field.
     */
    MODULE("modules", SoftwareModuleFields.ID.getFieldName(), SoftwareModuleFields.NAME.getFieldName()),
    /**
     * The tags field.
     */
    TAG("tags.name"),
    /**
     * The sw type key field.
     */
    TYPE("type.key"),
    /**
     * The metadata.
     */
    METADATA("metadata", new SimpleImmutableEntry<>("key", "value")),
    /**
     * The valid field.
     */
    VALID("valid");

    private final String fieldName;
    private boolean mapField;
    private Entry<String, String> subEntityMapTuple;

    private final List<String> subEntityAttributes;

    private DistributionSetFields(final String fieldName) {
        this(fieldName, false, null, Collections.emptyList());
    }

    private DistributionSetFields(final String fieldName, final String... subEntityAttributes) {
        this(fieldName, false, null, Arrays.asList(subEntityAttributes));
    }

    private DistributionSetFields(final String fieldName, final Entry<String, String> subEntityMapTuple) {
        this(fieldName, true, subEntityMapTuple, Collections.emptyList());
    }

    private DistributionSetFields(final String fieldName, final boolean mapField,
            final Entry<String, String> subEntityMapTuple, List<String> subEntityAttributes) {
        this.fieldName = fieldName;
        this.mapField = mapField;
        this.subEntityMapTuple = subEntityMapTuple;
        this.subEntityAttributes = subEntityAttributes;
    }

    @Override
    public List<String> getSubEntityAttributes() {
        return Collections.unmodifiableList(subEntityAttributes);
    }

    @Override
    public Optional<Entry<String, String>> getSubEntityMapTuple() {
        return Optional.ofNullable(subEntityMapTuple);
    }

    @Override
    public boolean isMap() {
        return mapField;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }
}
