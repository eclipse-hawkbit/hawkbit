/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.springframework.cloud.bus.event.entity;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 */
@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GenericEventEntity<E> implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty(required = true)
    private final String tenant;

    @JsonProperty(required = true)
    private final E genericId;

    @JsonProperty(required = true)
    private final String entityClass;

    private Map<String, PropertyChange> changeSetValues = new HashMap<>();

    /**
     * @param tenant
     * @param entityId
     */
    @JsonCreator
    public GenericEventEntity(@JsonProperty("tenant") final String tenant, @JsonProperty("genericId") final E genericId,
            @JsonProperty("entityClass") final String entityClass) {
        this.tenant = tenant;
        this.genericId = genericId;
        this.entityClass = entityClass;
    }

    /**
     * @return the genericId
     */
    public E getGenericId() {
        return genericId;
    }

    /**
     * @return the tenant
     */
    public String getTenant() {
        return tenant;
    }

    @Override
    public String toString() {
        return "EventEntity [tenant=" + tenant + ", entityId=" + genericId + ", entityClass=" + entityClass + "]";
    }

    public static void main(final String[] args) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();

        final GenericEventEntity<Long> value = new GenericEventEntity<>("TEST", 1L, Object.class.getName());
        value.getChangeSetValues().put("Test", new PropertyChange("asd", "aa"));

        final String json = mapper.writeValueAsString(value);

        final GenericEventEntity<Long> baseEntityIdSource = mapper.readValue(json, GenericEventEntity.class);

        System.out.println(baseEntityIdSource.getGenericId());

        System.out.println(baseEntityIdSource.getChangeSetValues().get("Test").getNewValue());

        final GenericEventEntity<List<Long>> values = new GenericEventEntity<>("TEST", Arrays.asList(1L, 2L),
                Object.class.getName());

        final String jsons = mapper.writeValueAsString(values);

        final GenericEventEntity<List<Long>> baseEntityIdSources = mapper.readValue(jsons, GenericEventEntity.class);
    }

    /**
     * @param changeSetValues
     *            the changeSetValues to set
     */
    public void setChangeSetValues(final Map<String, PropertyChange> changeSetValues) {
        this.changeSetValues = changeSetValues;
    }

    /**
     * @return the changeSetValues
     */
    public Map<String, PropertyChange> getChangeSetValues() {
        return changeSetValues;
    }

    /**
     * Carries old value and new value of a property .
     */
    @JsonInclude(Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PropertyChange {

        @JsonProperty(required = true)
        private final Object oldValue;
        @JsonProperty(required = true)
        private final Object newValue;

        /**
         * Initialize old value and new changes value of property.
         *
         * @param oldValue
         *            old value before change
         * @param newValue
         *            new value after change
         */
        @JsonCreator
        public PropertyChange(@JsonProperty("oldValue") final Object oldValue,
                @JsonProperty("newValue") final Object newValue) {
            super();
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        /**
         * @return the oldValue
         */
        public Object getOldValue() {
            return oldValue;
        }

        /**
         * @return the newValue
         */
        public Object getNewValue() {
            return newValue;
        }
    }
}
