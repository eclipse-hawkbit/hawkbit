/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.json;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The json model for the entity events.
 *
 * @param <I>
 *            the id type
 */
@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GenericEventEntity<I> {

    @JsonProperty(required = true)
    private final I genericId;

    @JsonProperty(required = true)
    private final String entityClass;

    private Map<String, PropertyChange> changeSetValues = new HashMap<>();

    /**
     * Constructor.
     * 
     * @param genericId
     *            the generic id.
     * @param entityClass
     *            the entity class.
     */
    @JsonCreator
    public GenericEventEntity(@JsonProperty("genericId") final I genericId,
            @JsonProperty("entityClass") final String entityClass) {
        this.genericId = genericId;
        this.entityClass = entityClass;
    }

    public I getGenericId() {
        return genericId;
    }

    public void setChangeSetValues(final Map<String, PropertyChange> changeSetValues) {
        this.changeSetValues = changeSetValues;
    }

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

        public Object getOldValue() {
            return oldValue;
        }

        public Object getNewValue() {
            return newValue;
        }
    }
}
