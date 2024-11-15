/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.utils;

import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeConverter;

public class MapAttributeConverter<JAVA_TYPE extends Enum<JAVA_TYPE>, DB_TYPE> implements AttributeConverter<JAVA_TYPE, DB_TYPE> {

    private final Map<JAVA_TYPE, DB_TYPE> javaToDbMap;
    private final Map<DB_TYPE, JAVA_TYPE> dbToJavaMap;

    protected MapAttributeConverter(final Map<JAVA_TYPE, DB_TYPE> javaToDbMap) {
        this.javaToDbMap = javaToDbMap;
        this.dbToJavaMap = javaToDbMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        if (javaToDbMap.size() != dbToJavaMap.size()) {
            throw new IllegalArgumentException("Duplicate values in javaToDbMap");
        }
    }

    @Override
    public DB_TYPE convertToDatabaseColumn(final JAVA_TYPE attribute) {
        return javaToDbMap.get(attribute);
    }

    @Override
    public JAVA_TYPE convertToEntityAttribute(final DB_TYPE dbData) {
        return dbToJavaMap.get(dbData);
    }
}
