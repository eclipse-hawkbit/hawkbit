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
 * A {@link TargetTag} is used to describe Target attributes and use them also
 * for filtering the target list.
 *
 *
 *
 *
 *
 *
 */
@Entity
@Table(name = "sp_target_tag", indexes = {
        @Index(name = "sp_idx_target_tag_prim", columnList = "tenant,id") }, uniqueConstraints = @UniqueConstraint(columnNames = {
                "name", "tenant" }, name = "uk_targ_tag"))
public class TargetTag extends Tag {
    private static final long serialVersionUID = 1L;

    @ManyToMany(mappedBy = "tags", targetEntity = Target.class, fetch = FetchType.LAZY)
    private List<Target> assignedToTargets;

    /**
     * Constructor.
     *
     * @param name
     *            of {@link TargetTag}
     * @param description
     *            of {@link TargetTag}
     * @param colour
     *            of {@link TargetTag}
     */
    public TargetTag(final String name, final String description, final String colour) {
        super(name, description, colour);
    }

    /**
     * Public constructor.
     *
     * @param name
     *            of the {@link TargetTag}
     **/
    public TargetTag(final String name) {
        super(name, null, null);
    }

    TargetTag() {
        super();
    }

    public List<Target> getAssignedToTargets() {
        return assignedToTargets;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + this.getClass().getName().hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof TargetTag)) {
            return false;
        }

        return true;
    }

}
