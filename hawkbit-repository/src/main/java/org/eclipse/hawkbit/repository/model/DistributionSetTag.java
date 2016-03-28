/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * A {@link DistributionSetTag} is used to describe DistributionSet attributes
 * and use them also for filtering the DistributionSet list.
 *
 */
@Entity
@Table(name = "sp_distributionset_tag", indexes = {
        @Index(name = "sp_idx_distribution_set_tag_prim", columnList = "tenant,id") }, uniqueConstraints = @UniqueConstraint(columnNames = {
                "name", "tenant" }, name = "uk_ds_tag"))
public class DistributionSetTag extends Tag {
    private static final long serialVersionUID = 1L;

    @ManyToMany(mappedBy = "tags", targetEntity = DistributionSet.class, fetch = FetchType.LAZY)
    private List<DistributionSet> assignedToDistributionSet;

    /**
     * Public constructor.
     *
     * @param name
     *            of the {@link DistributionSetTag}
     **/
    public DistributionSetTag(final String name) {
        super(name, null, null);
    }

    /**
     * Public constructor.
     *
     * @param name
     *            of the {@link DistributionSetTag}
     * @param description
     *            of the {@link DistributionSetTag}
     * @param colour
     *            of tag in UI
     */
    public DistributionSetTag(final String name, final String description, final String colour) {
        super(name, description, colour);
    }

    DistributionSetTag() {
        super();
    }

    public List<DistributionSet> getAssignedToDistributionSet() {
        return assignedToDistributionSet;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + this.getClass().getName().hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) { // NOSONAR - as this is generated
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof DistributionSetTag)) {
            return false;
        }

        return true;
    }
}
