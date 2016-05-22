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
 * ID class of the {@link Target} which contains the
 * {@link Target#getControllerId()} and the {@link Target#getName()} in one
 * object. Often it's necessary to remember the IDs of the {@link Target} and
 * the resolve for e.g. the UI the name of the target, this is very costly
 * operation, so it's much better if the ID and the name of the {@link Target}
 * is already in memory available.
 */
public class TargetIdName implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long targetId;
    private String controllerId;
    private String name;

    /**
     * @param targetId
     *            the ID of the target.
     * @param controllerId
     *            the {@link Target#getControllerId()}
     * @param name
     *            the {@link Target#getName()}
     */
    public TargetIdName(final long targetId, final String controllerId, final String name) {
        this.targetId = targetId;
        this.controllerId = controllerId;
        this.name = name;
    }

    public String getControllerId() {
        return controllerId;
    }

    public String getName() {
        return name;
    }

    public void setControllerId(final String id) {
        controllerId = id;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public long getTargetId() {
        return targetId;
    }

    @Override
    // Exception squid:S864 - generated code
    @SuppressWarnings("squid:S864")
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (targetId ^ targetId >>> 32);
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
        final TargetIdName other = (TargetIdName) obj;
        if (targetId != other.targetId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        // only return the ID because it's used in vaadin for setting the item
        // id in the dom
        return controllerId;
    }
}
