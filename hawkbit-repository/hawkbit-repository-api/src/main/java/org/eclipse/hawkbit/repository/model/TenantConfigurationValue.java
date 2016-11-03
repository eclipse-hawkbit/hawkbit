/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.io.Serializable;

/**
 * represents a tenant configuration value including some meta data
 * 
 * @param <T>
 *            type of the configuration value
 */
public final class TenantConfigurationValue<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = 1L;
    private T value;
    private Long lastModifiedAt;
    private String lastModifiedBy;
    private Long createdAt;
    private String createdBy;
    private boolean global = true;

    private TenantConfigurationValue() {
    }

    /**
     * Gets the value.
     *
     * @return the value
     */
    public T getValue() {
        return value;
    }

    /**
     * Checks if is global.
     *
     * @return true, if is global
     */
    public boolean isGlobal() {
        return global;
    }

    /**
     * Gets the last modified at.
     *
     * @return the last modified at
     */
    public Long getLastModifiedAt() {
        return lastModifiedAt;
    }

    /**
     * Gets the last modified by.
     *
     * @return the last modified by
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * Gets the created at.
     *
     * @return the created at
     */
    public Long getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the created by.
     *
     * @return the created by
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Builder.
     *
     * @param <K>
     *            the key type
     * @return the tenant configuration value builder
     */
    public static <K extends Serializable> TenantConfigurationValueBuilder<K> builder() {

        return new TenantConfigurationValueBuilder<>();
    }

    /**
     * builds the tenant configuration value including some meta data
     * 
     * @param <T>
     *            type of the configuration value
     */
    public static class TenantConfigurationValueBuilder<T extends Serializable> {

        private final TenantConfigurationValue<T> configuration = new TenantConfigurationValue<>();

        /**
         * Builds the.
         *
         * @return the tenant configuration value
         */
        public TenantConfigurationValue<T> build() {
            return configuration;
        }

        /**
         * sets the configuration value itself
         *
         * @param value
         *            the value
         * @return the tenant configuration value builder
         */
        public TenantConfigurationValueBuilder<T> value(final T value) {
            this.configuration.value = value;
            return this;
        }

        /**
         * set the is global attribute.
         *
         * @param global
         *            true when there is no tenant specific value, false
         *            otherwise
         * @return the tenant configuration value builder
         */
        public TenantConfigurationValueBuilder<T> global(final boolean global) {
            this.configuration.global = global;
            return this;
        }

        /**
         * Sets the last modified at attribute
         *
         * @param lastModifiedAt
         *            timestamp of last modification
         * @return the tenant configuration value builder
         */
        public TenantConfigurationValueBuilder<T> lastModifiedAt(final Long lastModifiedAt) {
            this.configuration.lastModifiedAt = lastModifiedAt;
            return this;
        }

        /**
         * sets the last modified by attribute
         *
         * @param lastModifiedBy
         *            the last modified by
         * @return the tenant configuration value builder
         */
        public TenantConfigurationValueBuilder<T> lastModifiedBy(final String lastModifiedBy) {
            this.configuration.lastModifiedBy = lastModifiedBy;
            return this;
        }

        /**
         * sets the created at attribute
         *
         * @param createdAt
         *            defined when the configuration has been created
         * @return the tenant configuration value builder
         */
        public TenantConfigurationValueBuilder<T> createdAt(final Long createdAt) {
            this.configuration.createdAt = createdAt;
            return this;
        }

        /**
         * sets the created by attribute
         *
         * @param createdBy
         *            defines by whom the configuration has been created
         * @return the tenant configuration value builder
         */
        public TenantConfigurationValueBuilder<T> createdBy(final String createdBy) {
            this.configuration.createdBy = createdBy;
            return this;
        }
    }
}
