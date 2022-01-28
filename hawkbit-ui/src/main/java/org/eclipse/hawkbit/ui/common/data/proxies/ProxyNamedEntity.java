/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.ui.common.data.aware.DescriptionAware;
import org.eclipse.hawkbit.ui.common.data.aware.NameAware;

/**
 * Proxy entity representing the {@link NamedEntity}, fetched from backend.
 */
public abstract class ProxyNamedEntity extends ProxyIdentifiableEntity implements NameAware, DescriptionAware {

    private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private String createdBy;
    private String lastModifiedBy;
    private String createdDate;
    private String modifiedDate;

    private Long createdAt;
    private Long lastModifiedAt;

    /**
     * Constructor
     */
    protected ProxyNamedEntity() {
    }

    /**
     * Constructor for ProxyNamedEntity
     *
     * @param id
     *          Id of named entity
     */
    protected ProxyNamedEntity(final Long id) {
        super(id);
    }

    /**
     * Gets the createdDate
     *
     * @return createdDate
     */
    public String getCreatedDate() {
        return createdDate;
    }

    /**
     * Sets the createdDate
     *
     * @param createdDate
     *          Date the entity is created
     */
    public void setCreatedDate(final String createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Gets the modifiedDate
     *
     * @return modifiedDate
     */
    public String getModifiedDate() {
        return modifiedDate;
    }

    /**
     * Sets the modifiedDate
     *
     * @param modifiedDate
     *          Date the entity is modified or updated
     */
    public void setModifiedDate(final String modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * Gets the user info who created the entity
     *
     * @return createdBy
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the createdBy
     *
     * @param createdBy
     *          user info who created the entity
     */
    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Gets the most recent user info who modified the entity
     *
     * @return lastModifiedBy
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * Sets the lastModifiedBy
     *
     * @param lastModifiedBy
     *          most recent user info who modified the entity
     */
    public void setLastModifiedBy(final String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * Gets the createdAt
     *
     * @return time when entity is created
     */
    public Long getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the createdAt
     *
     * @param createdAt
     *          time when entity is created
     */
    public void setCreatedAt(final Long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the lastModifiedAt
     *
     * @return time when entity is lastModified
     */
    public Long getLastModifiedAt() {
        return lastModifiedAt;
    }

    /**
     * Sets the lastModifiedAt
     *
     * @param lastModifiedAt
     *          time when entity is lastModified
     */
    public void setLastModifiedAt(final Long lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }
}
