/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.entity;

import java.io.Serializable;

import org.eclipse.hawkbit.repository.model.Target;

/**
 * Represent a {@link Target} data transfer object for the ui.
 */
public class TargetIdName implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long targetId;

    private final String controllerId;

    private final String targetName;

    /**
     * Constructor.
     * 
     * @param targetId
     *            the target id
     * @param controllerId
     *            the controller id
     * @param targetName
     *            target targetName
     */
    public TargetIdName(final Long targetId, final String controllerId, final String targetName) {
        this.targetId = targetId;
        this.controllerId = controllerId;
        this.targetName = targetName;
    }

    public String getControllerId() {
        return controllerId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((targetId == null) ? 0 : targetId.hashCode());
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
        if (!(obj instanceof TargetIdName)) {
            return false;
        }
        final TargetIdName other = (TargetIdName) obj;
        if (targetId == null) {
            if (other.targetId != null) {
                return false;
            }
        } else if (!targetId.equals(other.targetId)) {
            return false;
        }
        return true;
    }

}
