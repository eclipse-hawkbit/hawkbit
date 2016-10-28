/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.io.Serializable;

/**
 * The Software Module meta data composite key which contains the meta data key
 * and the ID of the software module itself.
 */
public final class SwMetadataCompositeKey implements Serializable {
    private static final long serialVersionUID = 1L;

    private String key;

    private Long softwareModule;

    /**
     * Default constructor for JPA.
     */
    public SwMetadataCompositeKey() {
        // Default constructor for JPA.
    }

    /**
     * @param moduleId
     *            the software module for this meta data
     * @param key
     *            the key of the meta data
     */
    public SwMetadataCompositeKey(final Long moduleId, final String key) {
        this.softwareModule = moduleId;
        this.key = key;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key
     *            the key to set
     */
    public void setKey(final String key) {
        this.key = key;
    }

    /**
     * @return the softwareModule
     */
    public Long getSoftwareModule() {
        return softwareModule;
    }

    /**
     * @param softwareModule
     *            the softwareModule to set
     */
    public void setSoftwareModule(final Long softwareModule) {
        this.softwareModule = softwareModule;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (key == null ? 0 : key.hashCode());
        result = prime * result + (softwareModule == null ? 0 : softwareModule.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) { // NOSONAR - as this is generated
                                              // code
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SwMetadataCompositeKey other = (SwMetadataCompositeKey) obj;
        if (key == null) {
            if (other.key != null) {
                return false;
            }
        } else if (!key.equals(other.key)) {
            return false;
        }
        if (softwareModule == null) {
            if (other.softwareModule != null) {
                return false;
            }
        } else if (!softwareModule.equals(other.softwareModule)) {
            return false;
        }
        return true;
    }

}
