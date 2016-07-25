/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import java.security.SecureRandom;

/**
 * 
 * Proxy for software module to display details in Software modules table.
 *
 *
 *
 */
public class ProxyBaseSoftwareModuleItem {

    private static final long serialVersionUID = -1555306616599140635L;

    private static final SecureRandom RANDOM_OBJ = new SecureRandom();

    private String nameAndVersion;

    private Long swId;

    private boolean assigned;

    private String createdDate;

    private String lastModifiedDate;

    private String createdByUser;

    private String modifiedByUser;

    private String name;
    private String version;
    private String vendor;
    private String description;

    /**
     * Default constructor.
     */
    public ProxyBaseSoftwareModuleItem() {
        super();
        swId = RANDOM_OBJ.nextLong();
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(final String vendor) {
        this.vendor = vendor;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getCreatedByUser() {
        return createdByUser;
    }

    public Long getSwId() {
        return swId;
    }

    public void setCreatedByUser(final String createdByUser) {
        this.createdByUser = createdByUser;
    }

    public void setSwId(final Long swId) {
        this.swId = swId;
    }

    public String getModifiedByUser() {
        return modifiedByUser;
    }

    public void setModifiedByUser(final String modifiedByUser) {
        this.modifiedByUser = modifiedByUser;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(final String createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(final String lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getNameAndVersion() {
        return nameAndVersion;
    }

    public void setNameAndVersion(final String nameAndVersion) {
        this.nameAndVersion = nameAndVersion;
    }

    public boolean isAssigned() {
        return assigned;
    }

    public void setAssigned(final boolean assigned) {
        this.assigned = assigned;
    }

}
