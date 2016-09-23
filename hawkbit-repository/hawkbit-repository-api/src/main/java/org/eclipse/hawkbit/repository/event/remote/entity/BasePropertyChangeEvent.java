/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Base remote property change event.
 *
 * @param <E>
 *            the entity
 */
public class BasePropertyChangeEvent<E extends TenantAwareBaseEntity> extends TenantAwareBaseEntityEvent<E> {

    private static final long serialVersionUID = -3671601415138242311L;

    private Map<String, PropertyChange> changeSetValues = new HashMap<>();

    /**
     * Constructor for json serialization.
     * 
     * @param tenant
     *            the tenant
     * @param entityId
     *            the entity id
     * @param entityClass
     *            the entity entityClassName
     * @param changeSetValues
     *            the property changes
     * @param applicationId
     *            the origin application id
     */
    @JsonCreator
    protected BasePropertyChangeEvent(@JsonProperty("tenant") final String tenant,
            @JsonProperty("entityId") final Long entityId,
            @JsonProperty("entityClass") final Class<? extends E> entityClass,
            @JsonProperty("changeSetValues") final Map<String, PropertyChange> changeSetValues,
            @JsonProperty("originService") final String applicationId) {
        super(tenant, entityId, entityClass, applicationId);
        this.changeSetValues = changeSetValues;
    }

    /**
     * Constructor.
     * 
     * @param entity
     *            the entity
     * @param changeSetValues
     *            the property changes
     * @param applicationId
     *            the origin application id
     */
    public BasePropertyChangeEvent(final E entity, final Map<String, PropertyChange> changeSetValues,
            final String applicationId) {
        super(entity, applicationId);
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
    public static class PropertyChange implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty(required = true)
        private final Serializable oldValue;
        @JsonProperty(required = true)
        private final Serializable newValue;

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
            this.oldValue = (Serializable) oldValue;
            this.newValue = (Serializable) newValue;
        }

        public Object getOldValue() {
            return oldValue;
        }

        public Object getNewValue() {
            return newValue;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((newValue == null) ? 0 : newValue.hashCode());
            result = prime * result + ((oldValue == null) ? 0 : oldValue.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PropertyChange other = (PropertyChange) obj;
            if (newValue == null) {
                if (other.newValue != null) {
                    return false;
                }
            } else if (!newValue.equals(other.newValue)) {
                return false;
            }
            if (oldValue == null) {
                if (other.oldValue != null) {
                    return false;
                }
            } else if (!oldValue.equals(other.oldValue)) {
                return false;
            }
            return true;
        }

    }

}
