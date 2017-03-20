/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Describing the fields of the Target model which can be used in the REST API
 * e.g. for sorting etc.
 *
 */
public enum TargetFields implements FieldNameProvider {

    /**
     * The controllerId field.
     */
    ID("controllerId"),

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
     * The createdAt field.
     */
    LASTMODIFIEDAT("lastModifiedAt"),
    /**
     * The controllerId field.
     */
    CONTROLLERID("controllerId"),
    /**
     * The updateStatus field.
     */
    UPDATESTATUS("updateStatus"),

    /**
     * The ip-address field.
     */
    IPADDRESS("address"),

    /**
     * The attribute map of target info.
     */
    ATTRIBUTE("controllerAttributes", true),

    /**
     * distribution sets which is assigned to the target.
     */
    ASSIGNEDDS("assignedDistributionSet", "name", "version"),

    /**
     * distribution sets which is installed on the target.
     */
    INSTALLEDDS("installedDistributionSet", "name", "version"),

    /**
     * The tags field.
     */
    TAG("tags.name"),

    /**
     * Last time the DDI or DMF client polled.
     */
    LASTCONTROLLERREQUESTAT("lastTargetQuery");

    private final String fieldName;
    private List<String> subEntityAttribues;
    private boolean mapField;

    TargetFields(final String fieldName) {
        this(fieldName, false, Collections.emptyList());
    }

    TargetFields(final String fieldName, final boolean isMapField) {
        this(fieldName, isMapField, Collections.emptyList());
    }

    TargetFields(final String fieldName, final String... subEntityAttribues) {
        this(fieldName, false, Arrays.asList(subEntityAttribues));
    }

    TargetFields(final String fieldName, final boolean mapField, final List<String> subEntityAttribues) {
        this.fieldName = fieldName;
        this.mapField = mapField;
        this.subEntityAttribues = subEntityAttribues;
    }

    @Override
    public List<String> getSubEntityAttributes() {
        return subEntityAttribues;
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
