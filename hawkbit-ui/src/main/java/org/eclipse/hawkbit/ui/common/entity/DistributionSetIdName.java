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

import org.eclipse.hawkbit.repository.model.DistributionSet;

/**
 * Represent a {@link DistributionSet} data transfer object for the ui.
 */
public class DistributionSetIdName implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String name;
    private final String version;

    /**
     * Constructor.
     * 
     * @param id
     *            the {@link DistributionSet#getId()}
     * @param name
     *            the {@link DistributionSet#getName()}
     * @param version
     *            the {@link DistributionSet#getVersion()}
     *
     */
    public DistributionSetIdName(final Long id, final String name, final String version) {
        this.id = id;
        this.name = name;
        this.version = version;
    }

    /**
     * Constructor.
     * 
     * @param distributionSet
     *            the distributionSet
     */
    public DistributionSetIdName(final DistributionSet distributionSet) {
        this(distributionSet.getId(), distributionSet.getName(), distributionSet.getVersion());
    }

    public Long getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
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
        final DistributionSetIdName other = (DistributionSetIdName) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        // only return the ID because it's used in vaadin for setting the item
        // id in the dom
        return id.toString();
    }
}
