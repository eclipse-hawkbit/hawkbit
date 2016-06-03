/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.eventbus.event;

import java.util.Map;

import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;

/**
 * Property change event.
 *
 * @param <E>
 */
public class AbstractPropertyChangeEvent<E extends TenantAwareBaseEntity> extends AbstractBaseEntityEvent<E> {

    private static final long serialVersionUID = -3671601415138242311L;
    private final transient Map<String, Values> changeSet;

    /**
     * Initialize base entity and property changed with old and new value.
     * 
     * @param baseEntity
     *            entity changed
     * @param changeSetValues
     *            details of properties changed and old value and new value of
     *            the changed properties
     */
    public AbstractPropertyChangeEvent(final E baseEntity, final Map<String, Values> changeSetValues) {
        super(baseEntity);
        this.changeSet = changeSetValues;
    }

    /**
     * @return the changeSet
     */
    public Map<String, Values> getChangeSet() {
        return changeSet;
    }

    /**
     * Carries old value and new value of a property .
     *
     */
    public class Values {
        private final Object oldValue;
        private final Object newValue;

        /**
         * Initialize old value and new changes value of property.
         * 
         * @param oldValue
         *            old value before change
         * @param newValue
         *            new value after change
         */
        public Values(final Object oldValue, final Object newValue) {
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
