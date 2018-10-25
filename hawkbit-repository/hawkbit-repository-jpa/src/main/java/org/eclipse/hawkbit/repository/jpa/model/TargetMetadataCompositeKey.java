/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * The Target Metadata composite key which contains the meta data key and the ID
 * of the Target itself.
 *
 */
public final class TargetMetadataCompositeKey implements Serializable {
    private static final long serialVersionUID = 1L;

    private String key;

    private Long target;

    public TargetMetadataCompositeKey() {
        // Default constructor for JPA.
    }

    /**
     * @param target
     *            the target Id for this meta data
     * @param key
     *            the key of the meta data
     */
    public TargetMetadataCompositeKey(final Long target, final String key) {
        this.target = target;
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public Long getTargetId() {
        return target;
    }

    public void setTargetId(final Long target) {
        this.target = target;
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, key);
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
        final TargetMetadataCompositeKey other = (TargetMetadataCompositeKey) obj;
        if (target == null) {
            if (other.target != null) {
                return false;
            }
        } else if (!target.equals(other.target)) {
            return false;
        }
        if (key == null) {
            if (other.key != null) {
                return false;
            }
        } else if (!key.equals(other.key)) {
            return false;
        }
        return true;
    }
}
